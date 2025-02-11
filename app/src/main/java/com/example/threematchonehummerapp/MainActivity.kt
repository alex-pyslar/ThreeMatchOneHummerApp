package com.example.threematchonehummerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

const val GRID_SIZE = 6

val colors = listOf(
    Color(0xFFE57373), // Red
    Color(0xFF81C784), // Green
    Color(0xFF64B5F6), // Blue
    Color(0xFFFFD54F), // Yellow
    Color(0xFFBA68C8)  // Purple
)

data class Tile(val color: Color)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Match3Game()
        }
    }
}

@Composable
fun Match3Game() {
    var grid by remember { mutableStateOf(generateGrid()) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var score by remember { mutableStateOf(0) }
    var scoreMultiplier by remember { mutableStateOf(1) }
    var passiveIncome by remember { mutableStateOf(0) }

    LaunchedEffect(passiveIncome) {
        while (true) {
            delay(5000)
            score += passiveIncome
        }
    }

    LaunchedEffect(grid) {
        delay(300)
        val matches = checkMatches(grid)
        if (matches.isNotEmpty()) {
            val gainedScore = matches.size * scoreMultiplier
            score += gainedScore
            grid = removeMatches(grid, matches)
            delay(300)
            grid = dropTiles(grid)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text("Три в ряд, один молоток", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Очки: $score", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Пассивный заработок: +$passiveIncome / 5s", fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(8.dp)) {
            for (row in 0 until GRID_SIZE) {
                Row {
                    for (col in 0 until GRID_SIZE) {
                        val tile = grid[row][col]
                        TileView(tile = tile) {
                            if (selected == null) {
                                selected = row to col
                            } else {
                                val (r1, c1) = selected!!
                                if ((r1 == row && (c1 - col).absoluteValue == 1) ||
                                    (c1 == col && (r1 - row).absoluteValue == 1)) {
                                    grid = swapTiles(grid, r1, c1, row, col)
                                    selected = null
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Магазин:", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (score >= 50) {
                score -= 50
                scoreMultiplier++
            }
        }) {
            Text("💰 Увеличить очки за тройки (+1) - 50")
        }

        Button(onClick = {
            if (score >= 100) {
                score -= 100
                passiveIncome++
            }
        }) {
            Text("⏳ Повышение пассивного дохода (+1) - 100")
        }
    }
}

@Composable
fun TileView(tile: Tile, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(55.dp)
            .padding(3.dp)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = tile.color,
                radius = size.minDimension / 2
            )
        }
    }
}

fun generateGrid(): MutableList<MutableList<Tile>> {
    return MutableList(GRID_SIZE) {
        MutableList(GRID_SIZE) { Tile(colors.random()) }
    }
}

fun swapTiles(grid: MutableList<MutableList<Tile>>, r1: Int, c1: Int, r2: Int, c2: Int): MutableList<MutableList<Tile>> {
    val newGrid = grid.map { it.toMutableList() }.toMutableList()
    val temp = newGrid[r1][c1]
    newGrid[r1][c1] = newGrid[r2][c2]
    newGrid[r2][c2] = temp
    return newGrid
}

fun checkMatches(grid: MutableList<MutableList<Tile>>): Set<Pair<Int, Int>> {
    val matches = mutableSetOf<Pair<Int, Int>>()
    for (r in 0 until GRID_SIZE) {
        for (c in 0 until GRID_SIZE - 2) {
            if (grid[r][c].color == grid[r][c + 1].color && grid[r][c].color == grid[r][c + 2].color) {
                matches.add(r to c)
                matches.add(r to c + 1)
                matches.add(r to c + 2)
            }
        }
    }
    for (c in 0 until GRID_SIZE) {
        for (r in 0 until GRID_SIZE - 2) {
            if (grid[r][c].color == grid[r + 1][c].color && grid[r][c].color == grid[r + 2][c].color) {
                matches.add(r to c)
                matches.add(r + 1 to c)
                matches.add(r + 2 to c)
            }
        }
    }
    return matches
}

fun removeMatches(grid: MutableList<MutableList<Tile>>, matches: Set<Pair<Int, Int>>): MutableList<MutableList<Tile>> {
    val newGrid = grid.map { it.toMutableList() }.toMutableList()
    matches.forEach { (r, c) -> newGrid[r][c] = Tile(Color.Transparent) }
    return newGrid
}

fun dropTiles(grid: MutableList<MutableList<Tile>>): MutableList<MutableList<Tile>> {
    val newGrid = grid.map { it.toMutableList() }.toMutableList()
    for (c in 0 until GRID_SIZE) {
        val column = newGrid.map { it[c] }.filter { it.color != Color.Transparent }
        val newColumn = MutableList(GRID_SIZE - column.size) { Tile(colors.random()) } + column
        for (r in 0 until GRID_SIZE) {
            newGrid[r][c] = newColumn[r]
        }
    }
    return newGrid
}