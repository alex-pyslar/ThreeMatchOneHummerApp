package com.example.threematchonehummerapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

const val GRID_SIZE = 6
const val TOTAL_TILES = GRID_SIZE * GRID_SIZE

data class Tile(val imageIndex: Int, val id: Long = Random.nextLong())

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
@Composable
fun Match3Game() {
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp * configuration.densityDpi / 160f
    val screenHeightPx = configuration.screenHeightDp * configuration.densityDpi / 160f

    var grid by remember { mutableStateOf(generateGrid()) }
    var score by remember { mutableStateOf(0) }
    var scoreMultiplier by remember { mutableStateOf(1) }
    var passiveIncome by remember { mutableStateOf(0) }
    var donationCurrency by remember { mutableStateOf(0) }
    var shopOpen by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
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

    // Falling background objects (snow-like effect)
    val fallingObjects = remember { mutableStateListOf<FallingObject>() }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(100L) // Более частое создание объектов
            if (fallingObjects.size < 120) { // Ограничение количества для производительности
                fallingObjects.add(FallingObject(bgPictures.random(), screenWidthPx))
            }
            // Удаление объектов, вышедших за пределы экрана
            fallingObjects.removeAll { it.y > screenHeightPx + 100f }
        }
    }

    // Обновление позиций объектов с учётом времени кадра
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            kotlinx.coroutines.delay(8L) // ~60 FPS
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000_000f // В секундах
            lastTime = currentTime
            fallingObjects.forEach { it.update(deltaTime) }
        }
    }

    // Passive income effect
    LaunchedEffect(passiveIncome) {
        while (true) {
            kotlinx.coroutines.delay(5000L)
            score += passiveIncome
        }
    }

    // Match processing
    LaunchedEffect(grid) {
        if (!isProcessing) {
            isProcessing = true
            var iteration = 0
            while (true) {
                iteration++
                if (iteration > 10) {
                    println("Warning: Reached max iterations in match processing")
                    break
                }
                val matches = checkMatches(grid)
                if (matches.isEmpty()) break
                score += matches.size * scoreMultiplier
                grid = removeMatches(grid, matches)
                grid = dropTiles(grid)
            }
            isProcessing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Falling objects layer (background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
        ) {
            fallingObjects.forEach { obj ->
                Icon(
                    painter = painterResource(id = obj.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .size((obj.size * 1.5f).dp)
                        .offset(x = obj.x.dp, y = obj.y.dp)
                        .rotate(obj.angle)
                        .alpha(0.8f),
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
                itemsIndexed(grid, key = { _, tile -> tile.id }) { index, tile ->
                    val row = index / GRID_SIZE
                    val col = index % GRID_SIZE
                    TileView(
                        tile = tile,
                        onSwipe = { direction ->
                            if (isProcessing || isAnimating) {
                                println("Swipe ignored: isProcessing = $isProcessing, isAnimating = $isAnimating")
                                return@TileView
                            }
                            println("Swiped at ($row, $col) direction: $direction")
                            val targetIndex = when (direction) {
                                SwipeDirection.RIGHT -> if (col < GRID_SIZE - 1) index + 1 else null
                                SwipeDirection.LEFT -> if (col > 0) index - 1 else null
                                SwipeDirection.UP -> if (row > 0) index - GRID_SIZE else null
                                SwipeDirection.DOWN -> if (row < GRID_SIZE - 1) index + GRID_SIZE else null
                            }
                            if (targetIndex != null) {
                                isAnimating = true
                                val newGrid = swapTiles(grid, index, targetIndex)
                                val matches = checkMatches(newGrid)
                                if (matches.isNotEmpty()) {
                                    grid = newGrid
                                    isAnimating = false
                                } else {
                                    grid = newGrid
                                    scope.launch {
                                        kotlinx.coroutines.delay(300L)
                                        grid = swapTiles(newGrid, index, targetIndex)
                                        isAnimating = false
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (donationCurrency >= 50) {
                    donationCurrency -= 50
                    passiveIncome++
                }
            }) {
                Text("Увеличить пассивный заработок - 50")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (donationCurrency >= 25) {
                    donationCurrency -= 25
                    scoreMultiplier++
                }
            }) {
                Text("Увеличить очки - 25")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { shopOpen = true }) {
                Text("Открыть магазин")
            }
        }

        // Shop
        if (shopOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .size(width = 350.dp, height = 500.dp)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Магазин",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        shopItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = item.first),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("+${item.second} валюты", fontSize = 16.sp)
                                    Button(onClick = { donationCurrency += item.second }) {
                                        Text("Купить (бесплатно)")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { shopOpen = false }) {
                            Text("Закрыть магазин")
                        }
                    }
                }
            }
        }
    }
}

enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN
}

@Composable
fun TileView(tile: Tile, onSwipe: (SwipeDirection) -> Unit) {
    val noteImages = listOf(
        R.drawable.note1,
        R.drawable.note2,
        R.drawable.note3,
        R.drawable.note4,
        R.drawable.note5
    )
    if (tile.imageIndex in 0..4) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .padding(3.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, offset ->
                        val direction = when {
                            offset.x > 30 -> SwipeDirection.RIGHT
                            offset.x < -30 -> SwipeDirection.LEFT
                            offset.y < -30 -> SwipeDirection.UP
                            offset.y > 30 -> SwipeDirection.DOWN
                            else -> null
                        }
                        direction?.let { onSwipe(it) }
                    }
                },
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
        Box(modifier = Modifier.size(55.dp).padding(3.dp))
    }
}

fun generateGrid(): List<Tile> {
    var grid = List(TOTAL_TILES) { Tile(imageIndex = Random.nextInt(0, 5)) }
    var attempts = 0
    while (checkMatches(grid).isNotEmpty()) {
        grid = List(TOTAL_TILES) { Tile(imageIndex = Random.nextInt(0, 5)) }
        attempts++
        if (attempts > 100) {
            println("Warning: Could not generate grid without matches after 100 attempts")
            break
        }
    }
    println("Initial grid: ${grid.chunked(GRID_SIZE).joinToString { row -> row.joinToString { it.imageIndex.toString() } }}")
    return grid
}

fun swapTiles(grid: List<Tile>, index1: Int, index2: Int): List<Tile> {
    val newGrid = grid.toMutableList()
    val temp = newGrid[index1]
    newGrid[index1] = newGrid[index2]
    newGrid[index2] = temp
    return newGrid
}

fun checkMatches(grid: List<Tile>): Set<Int> {
    val matches = mutableSetOf<Int>()
    // Horizontal matches
    for (row in 0 until GRID_SIZE) {
        var col = 0
        while (col < GRID_SIZE - 2) {
            val index = row * GRID_SIZE + col
            if (grid[index].imageIndex != -1 &&
                grid[index].imageIndex == grid[index + 1].imageIndex &&
                grid[index].imageIndex == grid[index + 2].imageIndex) {
                matches.add(index)
                matches.add(index + 1)
                matches.add(index + 2)
                col += 3
            } else {
                col++
            }
        }
    }
    // Vertical matches
    for (col in 0 until GRID_SIZE) {
        var row = 0
        while (row < GRID_SIZE - 2) {
            val index = row * GRID_SIZE + col
            if (grid[index].imageIndex != -1 &&
                grid[index].imageIndex == grid[index + GRID_SIZE].imageIndex &&
                grid[index].imageIndex == grid[index + 2 * GRID_SIZE].imageIndex) {
                matches.add(index)
                matches.add(index + GRID_SIZE)
                matches.add(index + 2 * GRID_SIZE)
                row += 3
            } else {
                row++
            }
        }
    }
    return matches
}

fun removeMatches(grid: List<Tile>, matches: Set<Int>): List<Tile> {
    val newGrid = grid.mapIndexed { index, tile ->
        if (index in matches) Tile(-1, Random.nextLong()) else tile
    }
    println("Grid after removeMatches: ${newGrid.chunked(GRID_SIZE).joinToString { row -> row.joinToString { it.imageIndex.toString() } }}")
    return newGrid
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun dropTiles(grid: List<Tile>): List<Tile> {
    val newGrid = MutableList(TOTAL_TILES) { Tile(-1) }
    for (col in 0 until GRID_SIZE) {
        val columnTiles = mutableListOf<Tile>()
        for (row in 0 until GRID_SIZE) {
            val index = row * GRID_SIZE + col
            if (grid[index].imageIndex != -1) {
                columnTiles.add(grid[index])
            }
        }
        for (row in GRID_SIZE - 1 downTo 0) {
            val index = row * GRID_SIZE + col
            if (columnTiles.isNotEmpty()) {
                newGrid[index] = columnTiles.removeLast()
            } else {
                newGrid[index] = Tile(Random.nextInt(0, 5), Random.nextLong())
            }
        }
    }
    println("Grid after dropTiles: ${newGrid.chunked(GRID_SIZE).joinToString { row -> row.joinToString { it.imageIndex.toString() } }}")
    return newGrid
}

data class FallingObject(
    val resId: Int,
    var x: Float,
    var y: Float = -100f,
    val size: Int = Random.nextInt(30, 75), // Уменьшенный размер для меньшей нагрузки
    var angle: Float = Random.nextFloat() * 360f,
    val speed: Float = Random.nextFloat() * 2f + 1.5f, // Ускоренное падение
    val rotationSpeed: Float = Random.nextFloat() * 2f - 1f // Случайная скорость вращения
) {
    constructor(resId: Int, screenWidthPx: Float) : this(
        resId,
        x = Random.nextFloat() * screenWidthPx
    )

    fun update(deltaTime: Float) {
        y += speed * deltaTime * 30f // Масштабирование скорости по времени кадра
        angle += rotationSpeed * deltaTime * 60f // Плавное вращение
        if (y > -100f) { // Логирование только для видимых объектов
            println("FallingObject: x=$x, y=$y, size=$size, resId=$resId")
        }
    }
}