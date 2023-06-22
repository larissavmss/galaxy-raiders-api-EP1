package galaxyraiders.core.game

import galaxyraiders.Config
import galaxyraiders.ports.RandomGenerator
import galaxyraiders.ports.ui.Controller
import galaxyraiders.ports.ui.Controller.PlayerCommand
import galaxyraiders.ports.ui.Visualizer

import kotlin.system.measureTimeMillis
import kotlin.collections.MutableList
import java.time.LocalDateTime
import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

const val MILLISECONDS_PER_SECOND: Int = 1000

object GameEngineConfig {
  private val config = Config(prefix = "GR__CORE__GAME__GAME_ENGINE__")

  val frameRate = config.get<Int>("FRAME_RATE")
  val spaceFieldWidth = config.get<Int>("SPACEFIELD_WIDTH")
  val spaceFieldHeight = config.get<Int>("SPACEFIELD_HEIGHT")
  val asteroidProbability = config.get<Double>("ASTEROID_PROBABILITY")
  val coefficientRestitution = config.get<Double>("COEFFICIENT_RESTITUTION")

  val msPerFrame: Int = MILLISECONDS_PER_SECOND / this.frameRate
}

@Suppress("TooManyFunctions")
class GameEngine(
  val generator: RandomGenerator,
  val controller: Controller,
  val visualizer: Visualizer,
) {
  data class ScoreboardDTO(
    val start: LocalDateTime,
    var finalPoints: Double,
    var asteroidsDestroyed: Int
  )

  data class LeaderboardDTO(
    var rankPosition: Int,
    val points: Double
  )

  val field = SpaceField(
    width = GameEngineConfig.spaceFieldWidth,
    height = GameEngineConfig.spaceFieldHeight,
    generator = generator
  )

  var currentGameExecution = ScoreboardDTO(
    start = LocalDateTime.now(),
    finalPoints = 0.0,
    asteroidsDestroyed = 0
  )
    private set

  var playing = true

  fun execute() {
    while (true) {
      val duration = measureTimeMillis { this.tick() }

      Thread.sleep(
        maxOf(0, GameEngineConfig.msPerFrame - duration)
      )
    }
  }

  fun execute(maxIterations: Int) {
    repeat(maxIterations) {
      this.tick()
    }
  }

  fun tick() {
    this.processPlayerInput()
    this.updateSpaceObjects()
    this.renderSpaceField()
  }

  fun updateScoreboard() {
    val objectMapper = ObjectMapper()
    // Read json
    val scoreboardString = File("../score/Scoreboard.json").readText(Charsets.UTF_8)
    val scoreboardList : MutableList<ScoreboardDTO> = objectMapper.readValue(scoreboardString)

    // Add current play
    scoreboardList.add(this.currentGameExecution)

    // Write json
    val newJsonString = objectMapper.writeValueAsString(scoreboardList)
    File("../score/Scoreboard.json").writeText(newJsonString)
  }

  fun updateLeaderboard() {
    val objectMapper = ObjectMapper()
    // Read json
    val leaderboardString = File("../score/Leaderboard.json").readText(Charsets.UTF_8)
    val leaderboardList : MutableList<LeaderboardDTO> = objectMapper.readValue(leaderboardString)

    // Check rank
    for(leader in leaderboardList) {
      if(this.currentGameExecution.finalPoints > leader.points) {
        var i = leader.rankPosition
        leaderboardList[i-1] = LeaderboardDTO(
          rankPosition = i,
          points = this.currentGameExecution.finalPoints
        )
        var currentLeader = leader
        currentLeader.rankPosition++
        while(i < leaderboardList.size) {
          var leaderCopy = leaderboardList[i]
          leaderboardList[i] = currentLeader
          currentLeader = leaderCopy
          currentLeader.rankPosition++
          i++
        }
        if(i < 3) {
          leaderboardList.add(currentLeader)
        }
        break
      }
    }
    if(leaderboardList.size == 0) {
      leaderboardList.add(LeaderboardDTO(
        rankPosition = 1,
        points = this.currentGameExecution.finalPoints
      ))
    }

    // Update leaderboard
    val newJsonString = objectMapper.writeValueAsString(leaderboardList)
    File("../score/Leaderboard.json").writeText(newJsonString)
  }

  fun processPlayerInput() {
    this.controller.nextPlayerCommand()?.also {
      when (it) {
        PlayerCommand.MOVE_SHIP_UP ->
          this.field.ship.boostUp()
        PlayerCommand.MOVE_SHIP_DOWN ->
          this.field.ship.boostDown()
        PlayerCommand.MOVE_SHIP_LEFT ->
          this.field.ship.boostLeft()
        PlayerCommand.MOVE_SHIP_RIGHT ->
          this.field.ship.boostRight()
        PlayerCommand.LAUNCH_MISSILE ->
          this.field.generateMissile()
        PlayerCommand.PAUSE_GAME -> {
          this.playing = !this.playing
          if(!this.playing) {
            this.updateLeaderboard()
            this.updateScoreboard()
          }
        }
      }
    }
  }

  fun updateSpaceObjects() {
    if (!this.playing) return
    this.field.resetExplosions()
    this.handleCollisions()
    this.moveSpaceObjects()
    this.trimSpaceObjects()
    this.generateAsteroids()
  }

  fun handleCollisions() {
    this.field.spaceObjects.forEachPair {
        (first, second) ->
      if (first.impacts(second)) {
        if( first.type == "Asteroid" && second.type == "Missile" ) {
          this.field.generateExplosion(first)
          this.updateCurrentGameExecution(first.radius + second.mass)
          this.field.removeSpaceObjectsFromField(first, second)
        } else if ( first.type == "Missile" && second.type == "Asteroid" ) {
          this.field.generateExplosion(second)
          this.updateCurrentGameExecution(second.radius + second.mass)
          this.field.removeSpaceObjectsFromField(second, first)
        } else {
          first.collideWith(second, GameEngineConfig.coefficientRestitution)
        }
      }
    }
  }

  fun moveSpaceObjects() {
    this.field.moveShip()
    this.field.moveAsteroids()
    this.field.moveMissiles()
  }

  fun trimSpaceObjects() {
    this.field.trimAsteroids()
    this.field.trimMissiles()
  }

  fun generateAsteroids() {
    val probability = generator.generateProbability()

    if (probability <= GameEngineConfig.asteroidProbability) {
      this.field.generateAsteroid()
    }
  }

  fun renderSpaceField() {
    this.visualizer.renderSpaceField(this.field)
  }

  private fun updateCurrentGameExecution(points: Double) {
    this.currentGameExecution.asteroidsDestroyed += 1
    this.currentGameExecution.finalPoints += points
  }
}

fun <T> List<T>.forEachPair(action: (Pair<T, T>) -> Unit) {
  for (i in 0 until this.size) {
    for (j in i + 1 until this.size) {
      action(Pair(this[i], this[j]))
    }
  }
}
