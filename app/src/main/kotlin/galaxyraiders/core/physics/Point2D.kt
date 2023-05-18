package galaxyraiders.core.physics

data class Point2D(val x: Double, val y: Double) {
  operator fun plus(p: Point2D): Point2D {
    val newPoint = Point2D(this.x + p.x, this.y + p.y)
    return newPoint
  }

  operator fun plus(v: Vector2D): Point2D {
    val newPoint = Point2D(this.x + v.dx, this.y + v.dy)
    return newPoint
  }

  override fun toString(): String {
    return "Point2D(x=$x, y=$y)"
  }

  fun toVector(): Vector2D {
    return Vector2D(this.x, this.y)
  }

  fun impactVector(p: Point2D): Vector2D {
    val dX = Math.abs(p.x - this.x)
    val dY = Math.abs(p.y - this.y)
    return Vector2D(dX, dY)
  }

  fun impactDirection(p: Point2D): Vector2D {
    return this.impactVector(p).unit
  }

  fun contactVector(p: Point2D): Vector2D {
    val vector = this.impactVector(p)
    return vector.normal
  }

  fun contactDirection(p: Point2D): Vector2D {
    return this.contactVector(p).unit
  }

  fun distance(p: Point2D): Double {
    val dX = Math.abs(p.x - this.x)
    val dY = Math.abs(p.y - this.y)
    val dist = Math.sqrt(dX*dX + dY*dY)
    return dist
  }
}
