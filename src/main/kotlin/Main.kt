import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlin.random.Random

fun main() = application {
	Window(
		onCloseRequest = ::exitApplication,
		title = "Game of Life Demo",
		state = rememberWindowState(width = 640.dp, height = 480.dp),
	) {
		val (board, setBoard) = remember {
			val board = Board(128, 128)
			board.randomPopulate()
			mutableStateOf(board, object : SnapshotMutationPolicy<Board> {
				override fun equivalent(a: Board, b: Board) = false
			})
		}
		val (going, setGoing) = remember { mutableStateOf(false) }
		val (step, setStep) = remember { mutableStateOf(0) }
		val (speed, setSpeed) = remember { mutableStateOf(0.5f) }

		LaunchedEffect(going, step) {
			if (going) {
				delay((speed * 600).toLong())
				board.doStep()
				setStep(step + 1)
			}
		}

		MaterialTheme {
			Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
				Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
					Button(onClick = {
						board.doStep()
						setBoard(board)
					}, enabled = !going) {
						Text("step")
					}

					Button(onClick = {
						for (i in 0 until 100) {
							board.doStep()
						}
						setBoard(board)
					}, enabled = !going) {
						Text("superstep")
					}

					Button(onClick = {
						setGoing(!going)
					}) {
						 Text(if (going) "stop" else "start")
					}

					Button(onClick = {
						board.randomPopulate()
						setGoing(false)
						setBoard(board)
					}) {
						Text("populate")
					}

					Button(onClick = {
						board.clear()
						setGoing(false)
						setBoard(board)
					}) {
						Text("clear")
					}

					Slider(speed, { setSpeed(it) }, valueRange = 0.0f.rangeTo(1.0f), steps = 12)
				}

				Canvas(modifier = Modifier.fillMaxWidth(1f).aspectRatio(
					ratio = board.width.toFloat() / board.height.toFloat(),
					matchHeightConstraintsFirst = true
				)
					.then(Modifier.pointerInput(Unit) {
					detectTapGestures { (x, y) ->
						if (going) return@detectTapGestures

						val width = size.width
						val height = size.height
						val cellWidth = width.toFloat() / board.width
						val cellHeight = height.toFloat() / board.height

						val gridX = (x / cellWidth).toInt()
						val gridY = (y / cellHeight).toInt()

						if (gridX in 0 until board.width && gridY in 0 until board.height) {
							if (!State.getAlive(board.getState()[board.index(gridX, gridY)])) {
								board.getState()[board.index(gridX, gridY)] = State.initial()
								board.getGenome()[board.index(gridX, gridY)] = Random.nextInt()
							} else {
								board.getState()[board.index(gridX, gridY)] = 0
							}
							setBoard(board)
						}
					}
				})) {
					val width = size.width
					val height = size.height
					val cellWidth = width / board.width
					val cellHeight = height / board.height

					for (y in 0 until board.height) {
						for (x in 0 until board.width) {
							if (State.getAlive(board.getState()[board.index(x, y)])) {
								drawRect(
									Color(Organism.getColor(board.getGenome()[board.index(x, y)])),
									Offset(x * cellWidth, y * cellHeight),
									Size(cellWidth, cellHeight)
								)
							}
						}
					}
				}
			}
		}
	}
}
