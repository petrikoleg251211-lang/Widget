package com.example.engine.game

import com.example.model.GameTemplateType
import com.example.model.MiniGameConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class EntityKind {
    PLAYER,
    HAZARD,
    CRYSTAL,
    BALL,
    AI_PADDLE,
    ORBIT_TARGET
}

data class GameEntity(
    val id: Int,
    val kind: EntityKind,
    val x: Float, // 0..300 arena coordinates
    val y: Float, // 0..300 arena coordinates
    val vx: Float = 0f,
    val vy: Float = 0f,
    val width: Float = 24f,
    val height: Float = 24f,
    val colorHex: String = "#00E5FF"
)

data class GameRuntimeState(
    val isRunning: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val score: Int = 0,
    val highScore: Int = 0,
    val level: Int = 1,
    val lives: Int = 3,
    val elapsedMs: Long = 0L,
    val playerX: Float = 150f,
    val playerY: Float = 255f,
    val orbitAngleRad: Float = 0f,
    val entities: List<GameEntity> = emptyList(),
    val statusBanner: String = "Натисніть СТАРТ або торкніться арени"
)

class MiniGameEngine {
    private var nextEntityId = 1

    fun createInitialState(config: MiniGameConfig): GameRuntimeState {
        val initialEntities = when (config.templateType) {
            GameTemplateType.ASTRO_DODGER -> listOf(
                GameEntity(nextEntityId++, EntityKind.CRYSTAL, 95f, 60f, 0f, 75f, 18f, 18f, "#00E676"),
                GameEntity(nextEntityId++, EntityKind.HAZARD, 185f, 30f, 0f, 95f, 26f, 26f, "#FF5252")
            )
            GameTemplateType.CYBER_PONG -> listOf(
                GameEntity(nextEntityId++, EntityKind.BALL, 150f, 150f, 115f, -135f, 16f, 16f, "#00E5FF"),
                GameEntity(nextEntityId++, EntityKind.AI_PADDLE, 150f, 32f, 0f, 0f, 68f, 12f, "#8C62FF")
            )
            GameTemplateType.ORBIT_DEFENDER -> listOf(
                GameEntity(nextEntityId++, EntityKind.ORBIT_TARGET, 150f, 60f, 0f, 0f, 26f, 26f, "#00E676")
            )
        }
        return GameRuntimeState(
            isRunning = false,
            isGameOver = false,
            isVictory = false,
            score = 0,
            highScore = config.highScore,
            level = 1,
            lives = 3,
            playerX = 150f,
            playerY = 258f,
            entities = initialEntities,
            statusBanner = "Готово: ${config.templateType.labelUk}"
        )
    }

    fun step(
        state: GameRuntimeState,
        config: MiniGameConfig,
        dtMs: Long
    ): GameRuntimeState {
        if (!state.isRunning || state.isGameOver || state.isVictory) return state

        val dtSec = (dtMs.coerceIn(8L, 64L)) / 1000f
        val speedMul = config.baseSpeed.coerceIn(0.4f, 3.0f) * (1f + (state.level - 1) * 0.15f)
        val newElapsed = state.elapsedMs + dtMs

        return when (config.templateType) {
            GameTemplateType.ASTRO_DODGER -> stepAstroDodger(state, config, dtSec, speedMul, newElapsed)
            GameTemplateType.CYBER_PONG -> stepCyberPong(state, config, dtSec, speedMul, newElapsed)
            GameTemplateType.ORBIT_DEFENDER -> stepOrbitDefender(state, config, dtSec, speedMul, newElapsed)
        }
    }

    private fun stepAstroDodger(
        state: GameRuntimeState,
        config: MiniGameConfig,
        dtSec: Float,
        speedMul: Float,
        newElapsed: Long
    ): GameRuntimeState {
        val updated = mutableListOf<GameEntity>()
        var score = state.score
        var lives = state.lives
        var banner = state.statusBanner

        val playerW = 42f
        val playerH = 20f

        for (ent in state.entities) {
            val ny = ent.y + (ent.vy * speedMul + config.gravity * 30f) * dtSec
            val nx = (ent.x + ent.vx * dtSec).coerceIn(20f, 280f)

            // Check AABB collision with player spaceship
            val hitX = abs(nx - state.playerX) < (ent.width + playerW) * 0.45f
            val hitY = abs(ny - state.playerY) < (ent.height + playerH) * 0.45f

            if (hitX && hitY) {
                if (ent.kind == EntityKind.CRYSTAL) {
                    score += 20
                    banner = "+20 Енергетичний кристал!"
                } else {
                    lives -= 1
                    banner = "Зіткнення з астероїдом! Залишилось життів: $lives"
                }
            } else if (ny < 305f) {
                updated.add(ent.copy(x = nx, y = ny))
            } else if (ent.kind == EntityKind.HAZARD) {
                score += 5 // Dodged asteroid
            }
        }

        // Spawn new entities periodically
        val spawnInterval = (config.spawnRateMs / speedMul).toLong().coerceAtLeast(280L)
        if (newElapsed / spawnInterval > state.elapsedMs / spawnInterval && updated.size < 10) {
            val isCrystal = Random.nextFloat() < 0.35f
            val spawnX = Random.nextInt(32, 268).toFloat()
            val vy = Random.nextInt(85, 145).toFloat()
            updated.add(
                GameEntity(
                    id = nextEntityId++,
                    kind = if (isCrystal) EntityKind.CRYSTAL else EntityKind.HAZARD,
                    x = spawnX,
                    y = 12f,
                    vx = Random.nextInt(-20, 21).toFloat(),
                    vy = vy,
                    width = if (isCrystal) 18f else 26f,
                    height = if (isCrystal) 18f else 26f,
                    colorHex = if (isCrystal) "#00E676" else "#FF5252"
                )
            )
        }

        val newHigh = max(state.highScore, score)
        val newLevel = 1 + (score / 60)
        val victory = score >= config.targetWinScore
        val gameOver = lives <= 0

        if (victory) banner = "ПЕРЕМОГА! Ціль ${config.targetWinScore} очок досягнута!"
        if (gameOver) banner = "ГРА ЗАВЕРШЕНА! Рахунок: $score"

        return state.copy(
            elapsedMs = newElapsed,
            score = score,
            highScore = newHigh,
            level = newLevel,
            lives = lives,
            isVictory = victory,
            isGameOver = gameOver,
            isRunning = !victory && !gameOver,
            entities = updated,
            statusBanner = banner
        )
    }

    private fun stepCyberPong(
        state: GameRuntimeState,
        config: MiniGameConfig,
        dtSec: Float,
        speedMul: Float,
        newElapsed: Long
    ): GameRuntimeState {
        val ball = state.entities.firstOrNull { it.kind == EntityKind.BALL }
            ?: GameEntity(nextEntityId++, EntityKind.BALL, 150f, 150f, 120f, -130f, 16f, 16f, "#00E5FF")
        val ai = state.entities.firstOrNull { it.kind == EntityKind.AI_PADDLE }
            ?: GameEntity(nextEntityId++, EntityKind.AI_PADDLE, 150f, 32f, 0f, 0f, 68f, 12f, "#8C62FF")

        var bx = ball.x + ball.vx * speedMul * dtSec
        var by = ball.y + ball.vy * speedMul * dtSec
        var bvx = ball.vx
        var bvy = ball.vy
        var score = state.score
        var lives = state.lives
        var banner = state.statusBanner

        // Bounce left/right walls
        if (bx <= 16f) {
            bx = 16f
            bvx = abs(bvx)
        } else if (bx >= 284f) {
            bx = 284f
            bvx = -abs(bvx)
        }

        // AI paddle tracks ball X smoothly
        val aiTargetX = (ai.x + (bx - ai.x) * (dtSec * 5.5f)).coerceIn(40f, 260f)

        // Bounce off AI paddle (top)
        if (by <= ai.y + 14f && bvy < 0f && abs(bx - aiTargetX) <= 44f) {
            by = ai.y + 15f
            bvy = abs(bvy) * 1.03f
        } else if (by < 10f) {
            // Player scored past AI
            score += 30
            banner = "+30 Гол у ворота AI!"
            bx = 150f
            by = 150f
            bvy = 130f
        }

        // Bounce off Player paddle (bottom)
        if (by >= state.playerY - 16f && by <= state.playerY + 12f && bvy > 0f) {
            if (abs(bx - state.playerX) <= 42f) {
                by = state.playerY - 17f
                bvy = -abs(bvy)
                val offset = (bx - state.playerX) / 36f
                bvx = (bvx + offset * 65f).coerceIn(-210f, 210f)
                score += 10
                banner = "+10 Відбиття імпульсу!"
            }
        }

        if (by > 292f) {
            lives -= 1
            banner = "Пропущено м'яч! Життів: $lives"
            bx = 150f
            by = 150f
            bvy = -130f
        }

        val newHigh = max(state.highScore, score)
        val newLevel = 1 + (score / 50)
        val victory = score >= config.targetWinScore
        val gameOver = lives <= 0

        return state.copy(
            elapsedMs = newElapsed,
            score = score,
            highScore = newHigh,
            level = newLevel,
            lives = lives,
            isVictory = victory,
            isGameOver = gameOver,
            isRunning = !victory && !gameOver,
            entities = listOf(
                ball.copy(x = bx, y = by, vx = bvx, vy = bvy),
                ai.copy(x = aiTargetX)
            ),
            statusBanner = banner
        )
    }

    private fun stepOrbitDefender(
        state: GameRuntimeState,
        config: MiniGameConfig,
        dtSec: Float,
        speedMul: Float,
        newElapsed: Long
    ): GameRuntimeState {
        val nextAngle = (state.orbitAngleRad + dtSec * 2.4f * speedMul) % (2f * Math.PI.toFloat())
        val radius = 92f
        val orbX = 150f + cos(nextAngle) * radius
        val orbY = 150f + sin(nextAngle) * radius
        return state.copy(
            elapsedMs = newElapsed,
            orbitAngleRad = nextAngle,
            playerX = orbX,
            playerY = orbY
        )
    }

    fun onTapOrAction(state: GameRuntimeState, config: MiniGameConfig): GameRuntimeState {
        if (!state.isRunning || state.isGameOver || state.isVictory) {
            return createInitialState(config).copy(
                isRunning = true,
                highScore = state.highScore,
                statusBanner = "Гру запущено!"
            )
        }
        if (config.templateType == GameTemplateType.ORBIT_DEFENDER) {
            val target = state.entities.firstOrNull { it.kind == EntityKind.ORBIT_TARGET } ?: return state
            val dist = hypot(state.playerX - target.x, state.playerY - target.y)
            return if (dist <= 44f) {
                val newScore = state.score + 25
                val nextTargetAngle = Random.nextFloat() * (2f * Math.PI.toFloat())
                val tx = 150f + cos(nextTargetAngle) * 92f
                val ty = 150f + sin(nextTargetAngle) * 92f
                val victory = newScore >= config.targetWinScore
                state.copy(
                    score = newScore,
                    highScore = max(state.highScore, newScore),
                    level = 1 + newScore / 50,
                    isVictory = victory,
                    isRunning = !victory,
                    entities = listOf(target.copy(x = tx, y = ty)),
                    statusBanner = if (victory) "ПЕРЕМОГА! Орбіту синхронізовано!" else "+25 Точний резонанс!"
                )
            } else {
                val nextLives = state.lives - 1
                state.copy(
                    lives = nextLives,
                    isGameOver = nextLives <= 0,
                    isRunning = nextLives > 0,
                    statusBanner = "Промах! Відстань ${dist.toInt()}px (Життів: $nextLives)"
                )
            }
        }
        return state
    }
}
