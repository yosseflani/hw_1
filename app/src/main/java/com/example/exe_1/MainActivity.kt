package com.example.exe_1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameLayout: FrameLayout
    private lateinit var carLayout: LinearLayout
    private lateinit var carImage: ImageView
    private lateinit var leftButton: ImageButton
    private lateinit var rightButton: ImageButton
    private lateinit var heartImages: Array<ImageView>

    private val obstacleList = ArrayList<ImageView>()
    private var carPosition = 1
    private val lanes = 3
    private var screenWidth = 0
    private var laneWidth = 0
    private var lives = 3
    private var isGameRunning = false
    private var isCollisionHandling = false
    private var lastObstacleTime = 0L
    private val timeBetweenObstacles = 4000L

    private val lanePositions = intArrayOf(0, 0, 0)

    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                createObstacle()
                moveObstacles()
                checkCollisions()
                handler.postDelayed(this, 700)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameLayout = findViewById(R.id.gameLayout)
        carLayout = findViewById(R.id.carLayout)
        carImage = findViewById(R.id.carImage)
        leftButton = findViewById(R.id.buttonLeft)
        rightButton = findViewById(R.id.buttonRight)

        leftButton.setImageResource(R.drawable.ic_arrow_left)
        rightButton.setImageResource(R.drawable.ic_arrow_right)

        heartImages = arrayOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )

        leftButton.setOnClickListener {
            if (carPosition > 0) {
                moveCarLeft()
            }
        }

        rightButton.setOnClickListener {
            if (carPosition < lanes - 1) {
                moveCarRight()
            }
        }

        gameLayout.post {
            screenWidth = gameLayout.width
            laneWidth = screenWidth / lanes

            for (i in 0 until lanes) {
                lanePositions[i] = i * laneWidth + laneWidth / 2
            }

            carPosition = 1
            positionCar()
            startGameLoop()
        }
    }

    private fun positionCar() {
        val carWidth = carImage.layoutParams.width.takeIf { it > 0 } ?: carImage.width.takeIf { it > 0 } ?: 50
        val leftMargin = lanePositions[carPosition] - carWidth / 2

        val params = FrameLayout.LayoutParams(
            carWidth,
            carImage.layoutParams.height
        )
        params.leftMargin = leftMargin
        params.topMargin = 0

        (carImage.parent as? ViewGroup)?.removeView(carImage)
        carImage.layoutParams = params
        carLayout.addView(carImage)
    }

    private fun moveCarLeft() {
        if (carPosition > 0) {
            carPosition--
            positionCar()
        }
    }

    private fun moveCarRight() {
        if (carPosition < lanes - 1) {
            carPosition++
            positionCar()
        }
    }

    private fun createObstacle() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastObstacleTime < timeBetweenObstacles) return
        lastObstacleTime = currentTime

        val lane = Random.nextInt(lanes)
        val obstacleSize = laneWidth * 3 / 6

        val obstacle = ImageView(this).apply {
            setImageResource(R.drawable.ic_obstacle)
            val params = FrameLayout.LayoutParams(
                obstacleSize,
                obstacleSize
            ).apply {
                leftMargin = lanePositions[lane] - obstacleSize / 2
                topMargin = 0
            }
            layoutParams = params
        }

        gameLayout.addView(obstacle)
        obstacleList.add(obstacle)
    }


    private fun moveObstacles() {
        val iterator = obstacleList.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            try {
                val params = obstacle.layoutParams as FrameLayout.LayoutParams
                params.topMargin += 15

                if (params.topMargin > gameLayout.height) {
                    gameLayout.removeView(obstacle)
                    iterator.remove()
                    continue
                }

                obstacle.layoutParams = params
            } catch (e: Exception) {
                e.printStackTrace()
                iterator.remove()
            }
        }
    }

    private fun checkCollisions() {
        if (isCollisionHandling) return

        val carLeft = lanePositions[carPosition] - carImage.width / 2
        val carRight = carLeft + carImage.width
        val carTop = gameLayout.height - carLayout.height
        val carBottom = carTop + carImage.height

        val iterator = obstacleList.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            try {
                val params = obstacle.layoutParams as FrameLayout.LayoutParams

                val obstacleLeft = params.leftMargin
                val obstacleRight = obstacleLeft + obstacle.width
                val obstacleTop = params.topMargin
                val obstacleBottom = obstacleTop + obstacle.height

                if (carLeft < obstacleRight && carRight > obstacleLeft &&
                    carTop < obstacleBottom && carBottom > obstacleTop) {

                    isCollisionHandling = true
                    Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()

                    gameLayout.removeView(obstacle)
                    iterator.remove()

                    vibrate()
                    decreaseLives()

                    handler.postDelayed({
                        isCollisionHandling = false
                    }, 500)

                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
                iterator.remove()
            }
        }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateHearts() {
        for (i in 0 until 3) {
            heartImages[i].setImageResource(
                if (i < lives) R.drawable.ic_heart else R.drawable.ic_empty
            )
        }
    }

    private fun decreaseLives() {
        lives--
        updateHearts()

        if (lives <= 0) {
            isGameRunning = false

            handler.postDelayed({
                resetGame()
            }, 2000)
        }
    }

    private fun startGameLoop() {
        isGameRunning = true
        handler.post(gameLoop)
    }

    private fun resetGame() {
        for (obstacle in obstacleList) {
            try {
                gameLayout.removeView(obstacle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        obstacleList.clear()

        carPosition = 1
        positionCar()

        lives = 3
        updateHearts()

        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        isGameRunning = false
        handler.removeCallbacks(gameLoop)
    }

    override fun onResume() {
        super.onResume()
        if (!isGameRunning) {
            startGameLoop()
        }
    }
}