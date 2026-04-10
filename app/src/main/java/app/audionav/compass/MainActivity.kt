/*
 * Copyright (C) 2025 Michael Whapples
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.audionav.compass.ui.theme.AudioNavCompassTheme
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Even when rejected works but notifications will not be shown.
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            AudioNavCompassTheme {
                val viewModel = koinViewModel<MainViewModel>()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainCompassScreen(
                        viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
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

@Composable
fun MainCompassScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        val compass = viewModel.compassConnection.collectAsState(CompassConnection.NoCompassConnection)
        when(compass.value) {
            CompassConnection.NoCompassConnection -> Text(text = "No compass available", style = MaterialTheme.typography.bodyLarge)
            is CompassConnection.ActiveCompassConnection -> {
                val heading = viewModel.heading.map { (it.roundToInt()) % 360 }.collectAsStateWithLifecycle(0).asIntState()
                val headingError = viewModel.headingError.collectAsStateWithLifecycle(1f).asFloatState()
                val course = viewModel.course.collectAsStateWithLifecycle(0)
                val audioState = viewModel.audioPlaying.collectAsState(false)
                Column(verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
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
                            Text(
                                text = "Heading error",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics {heading() }
                            )
                            LinearProgressIndicator(
                                progress = { headingError.floatValue },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        CompassCourseDisplay(course.value)
                    }
                    Row {
                        Button(onClick = { viewModel.updateAudioPlayingState(!audioState.value) }, modifier = Modifier.weight(1f)) {
                            Text(text = if (audioState.value) "Stop" else "Start")
                        }
                        Button(onClick = { viewModel.updateCourse(heading.intValue) }, modifier = Modifier.weight(1f)) {
                            Text(text = "Set course to heading")
                        }
                    }
                    CompassControlsBar(course.value, listOf(1, 5, 90), viewModel::updateCourse)
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

@Composable
fun CompassControlsBar(
    currentCourse: Int,
    increments: List<Int>,
    updateFunction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            for (step in increments.reversed()) {
                Button(onClick = { updateFunction(currentCourse - step)}, modifier = Modifier.weight(1f)) {
                    Text(text = "-$step",  modifier = Modifier.semantics { contentDescription = "Minus $step" })
                }
            }
            for (step in increments) {
                Button(onClick = { updateFunction(currentCourse + step)}, modifier = Modifier.weight(1f)) {
                    Text(text = "+$step",  modifier = Modifier.semantics { contentDescription = "Plus $step" })
                }
            }
        }
    }
}
