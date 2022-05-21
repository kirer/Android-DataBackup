#!/system/bin/sh

get_storage_space() {
  # $1: path
  df -h "$1" | sed -n 's|% /.*|%|p' | awk '{print $(NF-3),$(NF-2),$(NF)}' | sed 's/G//g' | awk 'END{print ""$2" GB/"$1" GB "$3}'
}

get_apk_path() {
  # $1: packageName
  apk_path="$(pm path "$1" | cut -f2 -d ':')"
  apk_path="$(echo "$apk_path" | head -1)"
  apk_path="${apk_path%/*}"
  if [ -z "$apk_path" ]; then
    unset apk_path
    return 1
  fi
  echo "$apk_path"
  unset apk_path
}

cd_to_path() {
  # $1: path
  cd "$1" || return 1
}

pv_force() {
  if [ -z "$1" ]; then
    pv -f -t -r -b
  else
    pv -f -t -r -b "$1"
  fi
}

compress_apk() {
  # $1: compression_type
  # $2: out_put
  mkdir -p "$2"
  case "$1" in
  tar) tar -cf "${2}/apk.tar" ./*.apk | pv_force ;;
  zstd) tar -cf - ./*apk | zstd -r -T0 --ultra -6 -q --priority=rt | pv_force >"${2}/apk.tar.zst" ;;
  lz4) tar -cf - ./*.apk | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 | pv_force >"${2}/apk.tar.lz4" ;;
  *) return 1 ;;
  esac
}

compress() {
  # $1: compression_type
  # $2: data_type
  # $3: package_name
  # $4: out_put
  mkdir -p "$4"
  am force-stop "$3"
  case "$2" in
  user)
    data_path="/data/data"
    if [ -d "$data_path/$3" ]; then
      case "$1" in
      tar) tar --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" -cpf - -C "${data_path}" "$3" | pv_force >"$4/$2.tar" ;;
      zstd) tar --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" -cpf - -C "${data_path}" "$3" | pv_force | zstd -r -T0 --ultra -6 -q --priority=rt >"$4/$2.tar.zst" ;;
      lz4) tar --exclude="$3/.ota" --exclude="$3/cache" --exclude="$3/lib" -cpf - -C "${data_path}" "$3" | pv_force | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 >"$4/$2.tar.lz4" ;;
      esac
    else
      echo "No such path: $data_path"
    fi
    ;;
  data | obb)
    data_path="/data/media/0/Android/$2"
    if [ -d "$data_path/$3" ]; then
      case "$1" in
      tar) tar --exclude="Backup_"* --exclude="$3/cache" -cPpf - "$data_path/$3" | pv_force >"$4/$2.tar" ;;
      zstd) tar --exclude="Backup_"* --exclude="$3/cache" -cPpf - "$data_path/$3" | pv_force | zstd -r -T0 --ultra -6 -q --priority=rt >"$4/$2.tar.zst" ;;
      lz4) tar --exclude="Backup_"* --exclude="$3/cache" -cPpf - "$data_path/$3" | pv_force | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 >"$4/$2.tar.lz4" ;;
      esac
    else
      echo "No such path: $data_path/$3"
    fi
    ;;
  esac
}

set_install_env() {
  settings put global verifier_verify_adb_installs 0
  settings put global package_verifier_enable 0
  if [ "$(settings get global package_verifier_user_consent)" != -1 ]; then
    settings put global package_verifier_user_consent -1
    settings put global upload_apk_enable 0
  fi
}

install_apk() {
  # $1: in_path
  # $2: package_name
  tmp_dir="/data/local/tmp/data_backup"
  rm -rf "$tmp_dir"
  mkdir -p "$tmp_dir"
  if [ -z "$(pm path "$2")" ]; then
    find "$1" -maxdepth 1 -name "apk.*" -type f | while read -r i; do
      case "${i##*.}" in
      tar) pv_force "$i" | tar -xmpf - -C "$tmp_dir" ;;
      zst | lz4) pv_force "$i" | tar -I zstd -xmpf - -C "$tmp_dir" ;;
      esac
    done
    apk_num=$(find "$tmp_dir" -maxdepth 1 -name "*.apk" -type f | wc -l)
    case "$apk_num" in
    1) pm install -i com.android.vending --user 0 -r ${tmp_dir}/*.apk ;;
    *)
      session=$(pm install-create -i com.android.vending --user 0 | grep -E -o '[0-9]+')
      find "$tmp_dir" -maxdepth 1 -name "*.apk" -type f | while read -r i; do
        pm install-write "$session" "${i##*/}" "$i"
      done
      pm install-commit "$session"
      ;;
    esac
  else
    return 1
  fi
  rm -rf "$tmp_dir"
}

set_owner_and_SELinux() {
  # $1: data_type
  # $2: package_name
  # $3: path
  case $1 in
  user)
    if [ -f /config/sdcardfs/$2/appid ]; then
      owner="$(cat "/config/sdcardfs/$2/appid")"
    else
      owner="$(dumpsys package "$2" | awk '/userId=/{print $1}' | cut -f2 -d '=' | head -1)"
    fi
    owner="$(echo "$owner" | grep -E -o '[0-9]+')"
    if [ "$owner" != "" ]; then
      chown -hR "$owner:$owner" "$3/"
      restorecon -RF "$3/"
    fi
    ;;
  data | obb) chmod -R 0777 "$3" ;;
  esac
}

decompress() {
  # $1: compression_type
  # $2: data_type
  # $3: input_path
  # $4: package_name
  am force-stop "$4"
  case "$2" in
  user)
    data_path="/data/data"
    case "$1" in
    tar)
      pv_force "$3" | tar --recursive-unlink -xmpf - -C "$data_path"
      ;;
    lz4 | zstd) pv_force "$3" | tar --recursive-unlink -I zstd -xmpf - -C "$data_path" ;;
    esac
    ;;
  data | obb | media)
    case "$1" in
    tar) pv_force "$3" | tar --recursive-unlink -xmPpf - ;;
    lz4 | zstd) pv_force "$3" | tar --recursive-unlink -I zstd -xmPpf - ;;
    esac
    ;;
  esac
}

get_app_version() {
  # $1: package_name
  dumpsys package "$1" | awk '/versionName=/{print $1}' | cut -f2 -d '=' | head -1
}

write_to_file() {
  # $1: content
  # $2: path
  echo "$1" >"$2"
}

compress_media() {
  # $1: compression_type
  # $2: input_path
  # $3: out_put
  mkdir -p "$3"
  if [ -d "$2" ]; then
    case "$1" in
    tar) tar --exclude="Backup_"* --exclude="${2##*/}/cache" -cPpf - "$2" | pv_force >"$3/${2##*/}.tar" ;;
    zstd) tar --exclude="Backup_"* --exclude="${2##*/}/cache" -cPpf - "$2" | pv_force | zstd -r -T0 --ultra -6 -q --priority=rt >"$3/${2##*/}.tar.zst" ;;
    lz4) tar --exclude="Backup_"* --exclude="${2##*/}/cache" -cPpf - "$2" | pv_force | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 >"$3/${2##*/}.tar.lz4" ;;
    esac
  else
    echo "No such path: $2"
  fi
}

check_bashrc() {
  echo "OK"
}

test_archive() {
  # $1: compression_type
  # $2: input_path
  if [ -e "$2" ]; then
    case "$1" in
    tar) tar -t -f "$2" ;;
    zstd | lz4) zstd -t "$2" ;;
    esac
  else
    echo "No such path: $2"
  fi
}