import com.example.render.{Renderable, AirTrack}

class Track(attributes: Map[String, String]) {

  val typeInfo = attributes.getOrElse("Type", "Unknown")
  val typeAttr = typeInfo.split("\\+")

  private val symbol: Renderable = new AirTrack() // 例としてAirTrackを使用

  def render(): Unit = {
    symbol.render()
  }

  def end(): Unit = {
    symbol.end()
  }
}