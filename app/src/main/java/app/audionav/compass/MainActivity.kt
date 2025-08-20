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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import app.audionav.compass.ui.theme.AudioNavCompassTheme
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            CompassConnection.NoCompassConnection -> Text(text = "No compass available", style = MaterialTheme.typography.bodyLarge, modifier = modifier)
            is CompassConnection.ActiveCompassConnection -> {
                val heading = (compass.value as CompassConnection.ActiveCompassConnection).headingInDegrees.map { (it.roundToInt()) % 360 }.collectAsState(0).asIntState()
                val course = (compass.value as CompassConnection.ActiveCompassConnection).course.collectAsState().asIntState()
                CompassHeading(heading.intValue, modifier = modifier)
                Button(onClick = { (compass.value as CompassConnection.ActiveCompassConnection).updateCourse(heading.intValue) }) {
                    Text(text = "Set course to heading", modifier = modifier)
                }
                CompassCourse(course.intValue, 1, 5, (compass.value as CompassConnection.ActiveCompassConnection)::updateCourse)
            }
        }
    }
}
@Composable
fun CompassHeading(heading: Int, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.current_heading),
            style = MaterialTheme.typography.headlineMedium,
            modifier = modifier.semantics { heading() }
        )
        Text(
            text = "%03d".format(heading),
            fontFamily = FontFamily(android.graphics.Typeface.MONOSPACE),
            modifier = modifier
        )
    }
}

@Composable
fun CompassCourse(
    currentCourse: Int,
    smallStep: Int,
    largeStep: Int,
    updateFunction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = "Course", style = MaterialTheme.typography.headlineMedium, modifier = modifier.semantics { heading() })
        Text(text = "%03d".format(currentCourse),  fontFamily = FontFamily(android.graphics.Typeface.MONOSPACE), modifier = modifier)
        Row {
            Button(onClick = { updateFunction(currentCourse - largeStep) }) {
                Text(text = "-$largeStep", modifier = modifier.semantics { contentDescription = "Minus $largeStep" })
            }
            Button(onClick = { updateFunction(currentCourse - smallStep)}) {
                Text(text = "-$smallStep", modifier = modifier.semantics { contentDescription = "Minus $smallStep" })
            }
            Button(onClick = { updateFunction(currentCourse + smallStep) }) {
                Text(text = "+$smallStep", modifier = modifier.semantics { contentDescription = "Plus $smallStep" })
            }
            Button(onClick = { updateFunction(currentCourse + largeStep) }) {
                Text(text = "+$largeStep", modifier = modifier.semantics { contentDescription = "Plus $largeStep" })
            }
        }
    }
}
