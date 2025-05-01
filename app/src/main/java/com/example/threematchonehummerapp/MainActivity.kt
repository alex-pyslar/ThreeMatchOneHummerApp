package com.example.threematchonehummerapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

const val GRID_SIZE = 6

data class Tile(val imageIndex: Int = 0, val id: Long = Random.nextLong())

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Match3Game()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Match3Game() {
    var grid by remember { mutableStateOf(generateGrid()) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var score by remember { mutableStateOf(0) }
    var scoreMultiplier by remember { mutableStateOf(1) }
    var passiveIncome by remember { mutableStateOf(0) }
    var donationCurrency by remember { mutableStateOf(0) }
    var shopOpen by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shopItems = listOf(
        Triple(R.drawable.shop1, 10, 0),
        Triple(R.drawable.shop2, 25, 0),
        Triple(R.drawable.shop3, 50, 0)
    )
    val bgColor = Color(0xFFFFE4EC)
    val bgPictures = listOf(
        R.drawable.bg_picture1,
        R.drawable.bg_picture2,
        R.drawable.bg_picture3,
        R.drawable.bg_picture4,
        R.drawable.bg_picture5,
    )

    // Falling objects
    val fallingObjects = remember { mutableStateListOf<FallingObject>() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(800L)
            fallingObjects.add(FallingObject(bgPictures.random()))
        }
    }
    LaunchedEffect(fallingObjects) {
        while (true) {
            delay(16L)
            fallingObjects.forEach { it.update() }
            fallingObjects.removeAll { it.y > 1000f }
        }
    }

    // Passive income effect
    LaunchedEffect(passiveIncome) {
        while (true) {
            delay(5000L)
            score += passiveIncome
        }
    }

    // Match processing
    LaunchedEffect(grid) {
        if (!isProcessing) {
            isProcessing = true
            delay(200L) // Небольшая задержка для предотвращения гонки
            var iteration = 0
            while (true) {
                iteration++
                if (iteration > 10) break // Защита от бесконечного цикла
                val matches = checkMatches(grid)
                if (matches.isEmpty()) break
                score += matches.size * scoreMultiplier
                grid = removeMatches(grid, matches)
                delay(500L) // Задержка для анимации исчезновения
                grid = dropTiles(grid)
                delay(500L) // Задержка для анимации падения
            }
            isProcessing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Falling objects layer
        Box(modifier = Modifier.matchParentSize().zIndex(0f)) {
            fallingObjects.forEach { obj ->
                Icon(
                    painter = painterResource(id = obj.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(obj.size.dp)
                        .offset(x = obj.x.dp, y = obj.y.dp)
                        .rotate(obj.angle)
                        .alpha(0.7f),
                    tint = Color.Unspecified
                )
            }
        }

        // Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Три в ряд, один молоток",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Донатная валюта: $donationCurrency", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.donation),
                    contentDescription = "Donation Icon",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Очки: $score", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.valuta),
                    contentDescription = "Valuta Icon",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
            Text("Пассивный заработок: +$passiveIncome / 5s", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            // Game grid
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_SIZE),
                state = gridState,
                modifier = Modifier
                    .size((GRID_SIZE * 55).dp)
            ) {
                itemsIndexed(grid.flatten(), key = { _, tile -> tile.id }) { index, tile ->
                    val row = index / GRID_SIZE
                    val col = index % GRID_SIZE
                    AnimatedVisibility(
                        visible = tile.imageIndex != -1,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        TileView(
                            tile = tile,
                            row = row,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween(500)
                            ),
                            onClick = {
                                if (isProcessing) return@TileView
                                if (selected == null) {
                                    selected = row to col
                                } else {
                                    val (r1, c1) = selected!!
                                    if ((r1 == row && (c1 - col).absoluteValue == 1) ||
                                        (c1 == col && (r1 - row).absoluteValue == 1)) {
                                        val newGrid = swapTiles(grid, r1, c1, row, col)
                                        val matches = checkMatches(newGrid)
                                        if (matches.isNotEmpty()) {
                                            grid = newGrid
                                        } else {
                                            grid = newGrid
                                            scope.launch {
                                                delay(300L)
                                                grid = swapTiles(newGrid, r1, c1, row, col)
                                            }
                                        }
                                        selected = null
                                    } else {
                                        selected = row to col
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (score >= 100) {
                    score -= 100
                    passiveIncome++
                }
            }) {
                Text("Купить пассивный заработок (+1) - 100")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (score >= 50) {
                    score -= 50
                    scoreMultiplier++
                }
            }) {
                Text("Увеличить очки за сборку 3 в ряд (+1) - 50")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { shopOpen = !shopOpen }) {
                Text(if (shopOpen) "Закрыть магазин" else "Открыть магазин")
            }
        }

        // Shop
        if (shopOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    shopItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.first),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("+${item.second} валюты", fontSize = 18.sp)
                                Button(onClick = { donationCurrency += item.second }) {
                                    Text("Купить (бесплатно)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TileView(tile: Tile, row: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val noteImages = listOf(
        R.drawable.note1,
        R.drawable.note2,
        R.drawable.note3,
        R.drawable.note4,
        R.drawable.note5
    )
    // Анимация падения для новых плиток
    val offsetY = remember { Animatable(if (tile.imageIndex != -1 && row < 2) -55f * (row + 1) else 0f) }
    LaunchedEffect(tile.id) {
        if (tile.imageIndex != -1 && row < 2) {
            offsetY.animateTo(0f, animationSpec = tween(500))
        }
    }
    if (tile.imageIndex in 0..4) {
        Box(
            modifier = modifier
                .size(55.dp)
                .padding(3.dp)
                .offset(y = with(LocalDensity.current) { offsetY.value.dp })
                .pointerInput(Unit) { detectTapGestures { onClick() } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = noteImages[tile.imageIndex]),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )
        }
    } else {
        Box(modifier = modifier.size(55.dp).padding(3.dp))
    }
}

fun generateGrid(): MutableList<MutableList<Tile>> {
    var grid = MutableList(GRID_SIZE) {
        MutableList(GRID_SIZE) { Tile(imageIndex = Random.nextInt(0, 5), id = Random.nextLong()) }
    }
    while (checkMatches(grid).isNotEmpty()) {
        grid = MutableList(GRID_SIZE) {
            MutableList(GRID_SIZE) { Tile(imageIndex = Random.nextInt(0, 5), id = Random.nextLong()) }
        }
    }
    return grid
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
    // Горизонтальные совпадения
    for (r in 0 until GRID_SIZE) {
        var c = 0
        while (c < GRID_SIZE - 2) {
            if (grid[r][c].imageIndex != -1 &&
                grid[r][c].imageIndex == grid[r][c + 1].imageIndex &&
                grid[r][c].imageIndex == grid[r][c + 2].imageIndex) {
                matches.add(r to c)
                matches.add(r to (c + 1))
                matches.add(r to (c + 2))
                c += 3
            } else {
                c++
            }
        }
    }
    // Вертикальные совпадения
    for (c in 0 until GRID_SIZE) {
        var r = 0
        while (r < GRID_SIZE - 2) {
            if (grid[r][c].imageIndex != -1 &&
                grid[r][c].imageIndex == grid[r + 1][c].imageIndex &&
                grid[r][c].imageIndex == grid[r + 2][c].imageIndex) {
                matches.add(r to c)
                matches.add((r + 1) to c)
                matches.add((r + 2) to c)
                r += 3
            } else {
                r++
            }
        }
    }
    return matches
}

fun removeMatches(grid: MutableList<MutableList<Tile>>, matches: Set<Pair<Int, Int>>): MutableList<MutableList<Tile>> {
    val newGrid = grid.map { it.toMutableList() }.toMutableList()
    matches.forEach { (r, c) -> newGrid[r][c] = Tile(-1, Random.nextLong()) }
    return newGrid
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun dropTiles(grid: MutableList<MutableList<Tile>>): MutableList<MutableList<Tile>> {
    val newGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { Tile(-1, Random.nextLong()) } }
    for (c in 0 until GRID_SIZE) {
        // Собираем валидные плитки (imageIndex != -1)
        val validTiles = mutableListOf<Tile>()
        for (r in 0 until GRID_SIZE) {
            if (grid[r][c].imageIndex != -1) {
                validTiles.add(grid[r][c])
            }
        }
        // Заполняем колонку снизу вверх
        for (r in GRID_SIZE - 1 downTo 0) {
            if (validTiles.isNotEmpty()) {
                newGrid[r][c] = validTiles.removeLast()
            } else {
                newGrid[r][c] = Tile(Random.nextInt(0, 5), Random.nextLong())
            }
        }
    }
    return newGrid
}

data class FallingObject(
    val resId: Int,
    var x: Float = Random.nextFloat() * 360f,
    var y: Float = -100f,
    val size: Int = Random.nextInt(32, 64),
    var angle: Float = Random.nextFloat() * 360f,
    val speed: Float = Random.nextFloat() * 2f + 1f
) {
    fun update() {
        y += speed
        angle += 1f
    }
}