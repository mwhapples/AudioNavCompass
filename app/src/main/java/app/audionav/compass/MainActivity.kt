/*
 * Copyright (C) 2025-2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package app.audionav.compass

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import app.audionav.compass.ui.theme.AudioNavCompassTheme
import app.audionav.compass.ui.theme.Typography
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Even when rejected works but notifications will not be shown.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            AudioNavCompassTheme {
                val viewModel = koinViewModel<MainViewModel>()
                val settings = CompassSettings(this)
                val onboardingVersion: Int by settings.onboardingVersionFlow()
                    .collectAsState(initial = 0)
                if (onboardingVersion < BuildConfig.VERSION_CODE) {
                    val coroutineScope = rememberCoroutineScope()
                    OnboardingScreen(onComplete = {
                        coroutineScope.launch {
                            settings.updateOnboardingVersion(BuildConfig.VERSION_CODE)
                        }
                    })
                } else {
                    AppScreen(viewModel)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingScreen(onComplete: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "${stringResource(R.string.app_name)} - Getting started")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    FloatingActionButton(onClick = onComplete) {
                        Icon(Icons.Default.Done, contentDescription = "Done")
                    }
                },
                actions = {}
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = "Welcome to ${stringResource(R.string.app_name)}",
                style = Typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text(text = "Whilst we hope you will find ${stringResource(R.string.app_name)} useful, due to the wide variety of devices available it is not possible to confirm how well the app will work on any specific device. In using the app you take responsibility for testing the app works to your satisfaction on your own devices.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen(viewModel: MainViewModel, windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.app_name))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            MainCompassScreen(
                viewModel,
                modifier = Modifier.padding(innerPadding), numOfColumns = when {
                    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 3
                    else -> 2
                }
            )
        }
    }
}

@Composable
fun MainCompassScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    numOfColumns: Int = 2
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        val compass =
            viewModel.compassConnection.collectAsState(CompassConnection.NoCompassConnection)
        when (compass.value) {
            CompassConnection.NoCompassConnection -> Text(
                text = "No compass available",
                style = MaterialTheme.typography.bodyLarge
            )

            is CompassConnection.ActiveCompassConnection -> {
                val heading = viewModel.heading.collectAsStateWithLifecycle(0).asIntState()
                val headingError =
                    viewModel.headingError.collectAsStateWithLifecycle(1f).asFloatState()
                val course = viewModel.course.collectAsStateWithLifecycle(0)
                val audioState = viewModel.audioPlaying.collectAsState(false)
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompassHeadingDisplay(heading.intValue)
                            HeadingErrorDisplay(headingError.floatValue)
                        }
                        CompassCourseDisplay(course.value)
                    }
                    CompassControls(increments = CourseIncrements(1, 5, 90), onIncrement = { viewModel.updateCourse(course.value + it) }, onCourseToHeading = { viewModel.updateCourse(heading.intValue) }, audioPlaying = audioState.value, onAudioChanged = { viewModel.updateAudioPlayingState(it) }, wideLayout = numOfColumns > 2)
                }
            }
        }
    }
}

@Composable
fun CompassHeadingDisplay(heading: Int, modifier: Modifier = Modifier) {
    BearingDisplay(label = stringResource(R.string.current_heading), bearing = heading)
}

@Composable
fun HeadingErrorDisplay(headingError: Float, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics { isTraversalGroup = true }
    ) {
        Text(
            text = stringResource(R.string.heading_error),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                heading()
                traversalIndex = 0f
            }
        )
        LinearProgressIndicator(
            progress = { headingError },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    traversalIndex = 1f
                }
        )
    }
}

@Composable
fun CompassCourseDisplay(course: Int, modifier: Modifier = Modifier) {
    BearingDisplay(label = stringResource(R.string.course), bearing = course)
}

@Composable
fun BearingDisplay(label: String, bearing: Int, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "%03d".format(bearing),
            fontFamily = FontFamily(android.graphics.Typeface.MONOSPACE),
            fontSize = 20.sp,
        )
    }
}

data class CourseIncrements(val small: Int, val medium: Int, val large: Int)

@Composable
fun CompassControls(increments: CourseIncrements, onIncrement: (Int) -> Unit, onCourseToHeading: () -> Unit, audioPlaying: Boolean, onAudioChanged: (Boolean) -> Unit, modifier: Modifier = Modifier, wideLayout: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.height(IntrinsicSize.Min)) {
        if (!wideLayout) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                AudioToggleButton(audioPlaying, onAudioChanged, modifier = Modifier.weight(1f).fillMaxHeight())
                SetCourseToHeadingButton(onCourseToHeading, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            PortButton(onIncrement, increments.large, modifier = Modifier.weight(1f))
            if (wideLayout) {
                AudioToggleButton(audioPlaying, onAudioChanged, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            StarboardButton(onIncrement, increments.large, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Row(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
                PortButton(onIncrement, increments.medium, modifier = Modifier.weight(1f))
                PortButton(onIncrement, increments.small, modifier = Modifier.weight(1f))
            }
            if (wideLayout) {
                SetCourseToHeadingButton(onCourseToHeading, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            Row(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
                StarboardButton(onIncrement, increments.small, modifier = Modifier.weight(1f))
                StarboardButton(onIncrement, increments.medium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SetCourseToHeadingButton(onCourseToHeading: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onCourseToHeading,
        modifier = modifier
    ) {
        Text(text = stringResource(R.string.set_course_to_heading), textAlign = TextAlign.Center)
    }
}

@Composable
private fun AudioToggleButton(
    audioPlaying: Boolean,
    onAudioChanged: (Boolean) -> Unit,modifier: Modifier = Modifier
) {
    Button(
        onClick = { onAudioChanged(!audioPlaying) },
        modifier = modifier
    ) {
        Text(
            text = if (audioPlaying) stringResource(R.string.stop_audio) else stringResource(
                R.string.start_audio
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PortButton(
    onIncrement: (Int) -> Unit,
    increment: Int,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onIncrement(-increment) },
        border = BorderStroke(width = 3.dp, Color.Red),
        modifier = modifier
    ) {
        Text(
            text = "-$increment",
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Minus $increment" })
    }
}

@Composable
private fun StarboardButton(
    onIncrement: (Int) -> Unit,
    increment: Int,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onIncrement(increment) },
        border = BorderStroke(width = 3.dp, Color.Green),
        modifier = modifier
    ) {
        Text(
            text = "+$increment",
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Plus $increment" })
    }
}
