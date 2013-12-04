require 'logger'
class LoggerSingleton
  @@logger = nil
  
  def self.logger
    if @@logger.nil?
      @@logger = Logger.new("ruby_log.log")
    end
    @@logger
  end
  private 
  def initialize
  end
end