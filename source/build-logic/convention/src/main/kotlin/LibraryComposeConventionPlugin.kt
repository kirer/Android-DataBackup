import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class LibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType<LibraryExtension>().apply {
                defaultConfig {
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = catalogLibs.findVersion("compose-compiler").get().toString()
                }

                dependencies {
                    add("implementation", catalogLibs.findLibrary("androidx.core.ktx").get())
                    add("implementation", catalogLibs.findLibrary("androidx.appcompat").get())
                    add("implementation", catalogLibs.findLibrary("androidx.lifecycle.runtime.ktx").get())
                    add("implementation", catalogLibs.findLibrary("androidx.activity.compose").get())
                    add("implementation", platform(catalogLibs.findLibrary("androidx-compose-bom").get()))
                    add("implementation", catalogLibs.findLibrary("androidx.compose.ui").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.ui.graphics").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.ui.tooling.preview").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.material3").get())
                    add("implementation", catalogLibs.findLibrary("androidx.compose.material.icons.extended").get())
                }
            }
        }
    }
}
