class Coordinate
  attr_reader :x, :y
  attr_writer :x, :y
  
  def initialize(x, y)
    @x = x
    @y = y
  end
  
end