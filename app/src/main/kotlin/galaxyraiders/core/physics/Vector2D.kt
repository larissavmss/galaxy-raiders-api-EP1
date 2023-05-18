package galaxyraiders.core.physics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("unit", "normal", "degree", "magnitude")
data class Vector2D(val dx: Double, val dy: Double) {
  override fun toString(): String {
    return "Vector2D(dx=$dx, dy=$dy)"
  }

  val magnitude: Double
    get() = Math.sqrt(dx*dx + dy*dy)

  val radiant: Double
    get() = Math.atan2(this.dy, this.dx)

  val degree: Double
    get() = this.radiant * 180 / Math.PI

  val unit: Vector2D
    get() = Vector2D(this.dx/Math.abs(this.magnitude), this.dy/Math.abs(this.magnitude))

  val normal: Vector2D
    get() = Vector2D(this.dy, this.dx*(-1)).unit

  operator fun times(scalar: Double): Vector2D {
    return Vector2D(this.dx*scalar, this.dy*scalar)
  }

  operator fun div(scalar: Double): Vector2D {
    return Vector2D(this.dx/scalar, this.dy/scalar)
  }

  operator fun times(v: Vector2D): Double {
    val result = this.dx*v.dx + this.dy*v.dy
    return result
  }

  operator fun plus(v: Vector2D): Vector2D {
    return Vector2D(this.dx + v.dx, this.dy + v.dy)
  }

  operator fun plus(p: Point2D): Point2D {
    return Point2D(this.dx + p.x, this.dy + p.y)
  }

  operator fun unaryMinus(): Vector2D {
    return Vector2D(this.dx*(-1), this.dy*(-1))
  }

  operator fun minus(v: Vector2D): Vector2D {
    return Vector2D(this.dx - v.dx, this.dy - v.dy)
  }

  fun scalarProject(target: Vector2D): Double {
    return this * target.unit
  }

  fun vectorProject(target: Vector2D): Vector2D {
    val scalarProj = this.scalarProject(target)
    return target.unit * scalarProj
  }
}

operator fun Double.times(v: Vector2D): Vector2D {
  return Vector2D(this * v.dx, this * v.dy)
}
