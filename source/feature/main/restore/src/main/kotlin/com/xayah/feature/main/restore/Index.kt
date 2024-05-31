package com.xayah.feature.main.restore

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.StorageMode
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.PackageIcons
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.select
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.getValue
import com.xayah.core.ui.util.icon

@SuppressLint("StringFormatInvalid")
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastRestoreTime by viewModel.lastRestoreTimeState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.UpdateApps)
    }

    RestoreScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.restore),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OverviewLastRestoreCard(
                modifier = Modifier.padding(SizeTokens.Level16),
                lastRestoreTime = lastRestoreTime
            )

            var enabled by remember { mutableStateOf(true) }
            val storageOptions = listOf(context.getString(R.string.local), context.getString(R.string.cloud))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level16)
                    .paddingBottom(SizeTokens.Level16),
            ) {
                storageOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        enabled = enabled,
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = storageOptions.size),
                        onClick = {
                            viewModel.launchOnIO {
                                enabled = false
                                viewModel.emitState(state = uiState.copy(storageIndex = index, storageType = if (index == 0) StorageMode.Local else StorageMode.Cloud))
                                viewModel.emitIntent(IndexUiIntent.UpdateApps)
                                enabled = true
                            }
                        },
                        selected = index == uiState.storageIndex
                    ) {
                        Text(label)
                    }
                }
            }

            AnimatedVisibility(uiState.storageIndex == 1) {
                if (accounts.isEmpty()) {
                    Clickable(
                        title = StringResourceToken.fromStringId(R.string.account),
                        value = StringResourceToken.fromStringId(R.string.no_available_account),
                        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cancel_circle),
                        trailingIcon = ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowRight),
                    ) {
                        navController.navigate(MainRoutes.Cloud.route)
                    }
                } else {
                    val dialogState = LocalSlotScope.current!!.dialogSlot
                    var currentIndex by remember { mutableIntStateOf(if (uiState.cloudEntity == null) 0 else accounts.indexOfFirst { it.title.getValue(context) == uiState.cloudEntity!!.name }) }
                    LaunchedEffect(currentIndex) {
                        viewModel.emitIntent(IndexUiIntent.SetCloudEntity(name = accounts[currentIndex].title.getValue(context)))
                    }
                    Selectable(
                        title = StringResourceToken.fromStringId(R.string.account),
                        leadingIcon = uiState.cloudEntity?.type?.icon ?: ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                        value = if (uiState.cloudEntity == null) StringResourceToken.fromStringId(R.string.choose_an_account) else accounts[currentIndex].desc
                            ?: StringResourceToken.fromStringId(R.string.unknown),
                        current = if (uiState.cloudEntity == null) StringResourceToken.fromStringId(R.string.not_selected) else accounts[currentIndex].title
                    ) {
                        viewModel.launchOnIO {
                            val (state, selectedIndex) = dialogState.select(
                                title = StringResourceToken.fromStringId(R.string.account),
                                defIndex = currentIndex,
                                items = accounts
                            )
                            if (state) {
                                currentIndex = selectedIndex
                            }
                        }
                    }
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            Clickable(
                title = StringResourceToken.fromStringId(R.string.apps),
                value = if (uiState.packages.isEmpty()) null else StringResourceToken.fromString(
                    "${context.getString(R.string.args_apps_backed_up, uiState.packages.size)}${if (uiState.packagesSize.isNotEmpty()) " (${uiState.packagesSize})" else ""}"
                ),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_apps),
                interactionSource = interactionSource,
                content = if (uiState.packages.isEmpty()) null else {
                    {
                        PackageIcons(modifier = Modifier.paddingTop(SizeTokens.Level8), packages = uiState.packages, interactionSource = interactionSource) {
                            viewModel.emitIntentOnIO(IndexUiIntent.ToAppList(navController))
                        }
                    }
                }
            ) {
                viewModel.emitIntentOnIO(IndexUiIntent.ToAppList(navController))
            }

            Title(title = StringResourceToken.fromStringId(R.string.advanced)) {
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.reload),
                    value = StringResourceToken.fromStringId(R.string.reload_desc),
                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.ManageSearch),
                ) {
                    viewModel.emitIntentOnIO(IndexUiIntent.ToReload(navController))
                }
            }
        }
    }
}
