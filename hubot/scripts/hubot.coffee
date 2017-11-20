# Description:
#   Interact with hubot itself.
#
# Dependencies:
#   None
#
# Configuration:
#   None

# Commands:
#   None

# URLS:
#   POST /hubot/notify/<room> (message=<message>)
#
# Author:
#   Fabric8, nrayapati

module.exports = (robot) ->

  fs = require 'fs'
  fs.exists './logs/', (exists) ->
    if exists
      startLogging()
    else
      fs.mkdir './logs/', (error) ->
        unless error
          startLogging()
        else
          console.log "Could not create logs directory: #{error}"
  startLogging = ->
    console.log "Started ChatOps HTTP Script logging"
    robot.hear //, (msg) ->
      fs.appendFile logFileName(msg), formatMessage(msg), (error) ->
        console.log "Could not log message: #{error}" if error
  logFileName = (msg) ->
    safe_room_name = "#{msg.message.room}".replace /[^a-z0-9]/ig, ''
    "./logs/#{safe_room_name}.log"
  formatMessage = (msg) ->
    "[#{new Date()}] #{msg.message.user.name}: #{msg.message.text}\n"

  robot.router.post '/hubot/notify/:room', (req, res) ->
    room = req.params.room
    message = req.body.message
    robot.messageRoom room, "```"+message+"```"
    res.end()
