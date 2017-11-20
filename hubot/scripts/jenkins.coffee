# Description:
#   Interact with your Jenkins CI server
#
# Dependencies:
#   None
#
# Configuration:
#   HUBOT_JENKINS_URL
#   HUBOT_JENKINS_AUTH - A reference to where the auth token can be found on the file system
##
# Commands:
#   hubot jenkins abort <jobPath> - aborts the given job that is paused, waiting on user input
#   hubot jenkins build <jobPath> - builds the specified Jenkins job
#   hubot jenkins build <jobPath>, <params> - builds the specified Jenkins job with parameters as key=value&key2=value2
#   hubot jenkins desc <jobPath> - Describes the specified Jenkins job
#   hubot jenkins last <jobPath> - Details about the last build for the specified Jenkins job
#   hubot jenkins list <filter> - lists Jenkins jobs
#   hubot jenkins proceed <jobPath> - proceeds with the given job that is paused, waiting on user input
#
# Author:
#   dougcole, nrayapati

querystring = require 'querystring'

jobList = []

jenkinsBuild = (msg, buildWithEmptyParameters) ->
    url = process.env.HUBOT_JENKINS_URL
    jobPath = querystring.escape msg.match[1]
    jobPath = jobPath.split("%2F").join("/")
    params = msg.match[3]
    command = if buildWithEmptyParameters then "buildWithParameters" else "build"
    path = if params then "#{url}#{jobPath}buildWithParameters?#{params}" else "#{url}#{jobPath}#{command}"

    req = msg.http(path)

    if process.env.HUBOT_JENKINS_AUTH
      fs = require('fs');
      file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
      auth = file.toString()
      req.headers Authorization: "Bearer #{auth}"

    req.header('Content-Length', 0)
    req.post() (err, res, body) ->
        if err
          msg.reply "```Jenkins says 1: #{err}```"
        else if 200 <= res.statusCode < 400 # Or, not an error code.
          msg.reply "```(#{res.statusCode}) Build started for #{url}#{jobPath} ```"
        else if 400 == res.statusCode
          jenkinsBuild(msg, true)
        else if 404 == res.statusCode
          msg.reply "```Build not found, double check that it exists and is spelt correctly.```"
        else
          msg.reply "```Jenkins says: Status #{res.statusCode} #{body}```"

jenkinsProceed = (msg, buildWithEmptyParameters) ->
    url = process.env.HUBOT_JENKINS_URL
    jobPath = querystring.escape msg.match[1]
    jobPath = jobPath.split("%2F").join("/")
    inputId = 'Proceed'
    command = if buildWithEmptyParameters then "buildWithParameters" else "build"
    path = "#{url}#{jobPath}input/#{inputId}/proceedEmpty"

    req = msg.http(path)

    # kmcanoy alterations - Read token from system. Use Bearer instead of Basic

    if process.env.HUBOT_JENKINS_AUTH
      fs = require('fs');
      file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
      auth = file.toString()
      req.headers Authorization: "Bearer #{auth}"

    req.header('Content-Length', 0)
    req.post() (err, res, body) ->
        if err
          msg.reply "```Jenkins says 2: #{err} #{auth}```"
        else if 200 <= res.statusCode < 400 # Or, not an error code.
          msg.reply "```(#{res.statusCode}) Proceeding with #{url}#{jobPath} ```"
        else if 400 == res.statusCode
          jenkinsBuild(msg, true)
        else if 404 == res.statusCode
          msg.reply "```Build not found, double check that it exists and is spelt correctly.```"
        else
          msg.reply "```Jenkins says: Status #{res.statusCode} #{body}```"

jenkinsAbort = (msg, buildWithEmptyParameters) ->
    url = process.env.HUBOT_JENKINS_URL
    jobPath = querystring.escape msg.match[1]
    jobPath = jobPath.split("%2F").join("/")
    inputId = 'Proceed'
    command = if buildWithEmptyParameters then "buildWithParameters" else "build"
    path = "#{url}#{jobPath}input/#{inputId}/abort"

    req = msg.http(path)

    if process.env.HUBOT_JENKINS_AUTH
      fs = require('fs');
      file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
      auth = file.toString()
      req.headers Authorization: "Bearer #{auth}"

    req.header('Content-Length', 0)
    req.post() (err, res, body) ->
        if err
          msg.reply "```Jenkins says 3: #{err}```"
        else if 200 <= res.statusCode < 400 # Or, not an error code.
          msg.reply "```(#{res.statusCode}) Aborting #{url}#{jobPath} ```"
        else if 400 == res.statusCode
          jenkinsBuild(msg, true)
        else if 404 == res.statusCode
          msg.reply "```Build not found, double check that it exists and is spelt correctly.```"
        else
          msg.reply "```Jenkins says: Status #{res.statusCode} #{body}```"

jenkinsDescribe = (msg) ->
    url = process.env.HUBOT_JENKINS_URL
    jobPath = querystring.escape msg.match[1]
    jobPath = jobPath.split("%2F").join("/")
    path = "#{url}#{jobPath}api/json"

    req = msg.http(path)

    if process.env.HUBOT_JENKINS_AUTH
      fs = require('fs');
      file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
      auth = file.toString()
      req.headers Authorization: "Bearer #{auth}"

    req.header('Content-Length', 0)
    req.get() (err, res, body) ->
        if err
          msg.send "```Jenkins says 4: #{err}```"
        else
          response = ""
          try
            content = JSON.parse(body)
            response += "JOB: #{content.displayName}\n"
            response += "URL: #{content.url}\n"

            if content.description
              response += "DESCRIPTION: #{content.description}\n"

            response += "ENABLED: #{content.buildable}\n"
            response += "STATUS: #{content.color}\n"

            tmpReport = ""
            if content.healthReport.length > 0
              for report in content.healthReport
                tmpReport += "\n  #{report.description}"
            else
              tmpReport = " unknown"
            response += "HEALTH: #{tmpReport}\n"

            parameters = ""
            for item in content.actions
              if item.parameterDefinitions
                for param in item.parameterDefinitions
                  tmpDescription = if param.description then " - #{param.description} " else ""
                  tmpDefault = if param.defaultParameterValue then " (default=#{param.defaultParameterValue.value})" else ""
                  parameters += "\n  #{param.name}#{tmpDescription}#{tmpDefault}"

            if parameters != ""
              response += "PARAMETERS: #{parameters}\n"

            msg.send "```"+response+"```"

            if not content.lastBuild
              return

            path = "#{url}#{jobPath}#{content.lastBuild.number}/api/json"
            req = msg.http(path)
            if process.env.HUBOT_JENKINS_AUTH
              fs = require('fs');
              file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
              auth = file.toString()
              req.headers Authorization: "Bearer #{auth}"

            req.header('Content-Length', 0)
            req.get() (err, res, body) ->
                if err
                  msg.send "Jenkins says 5: #{err}"
                else
                  response = ""
                  try
                    content = JSON.parse(body)
                    console.log(JSON.stringify(content, null, 4))
                    jobstatus = content.result || 'PENDING'
                    jobdate = new Date(content.timestamp);
                    response += "LAST JOB: #{jobstatus}, #{jobdate}\n"

                    msg.send response
                  catch error
                    msg.send error

          catch error
            msg.send error

jenkinsLast = (msg) ->
    url = process.env.HUBOT_JENKINS_URL
    jobPath = querystring.escape msg.match[1]
    jobPath = jobPath.split("%2F").join("/")

    path = "#{url}#{jobPath}lastBuild/api/json"

    req = msg.http(path)

    if process.env.HUBOT_JENKINS_AUTH
      fs = require('fs');
      file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
      auth = file.toString()
      req.headers Authorization: "Bearer #{auth}"

    req.header('Content-Length', 0)
    req.get() (err, res, body) ->
        if err
          msg.send "Jenkins says 6: #{err}"
        else
          response = ""
          try
            content = JSON.parse(body)
            response += "NAME: #{content.fullDisplayName}\n"
            response += "URL: #{content.url}\n"

            if content.description
              response += "DESCRIPTION: #{content.description}\n"

            response += "BUILDING: #{content.building}\n"

            msg.send response

# TODO enhance this to support iterate through folders and don't allow empty input as it can cause big mess
# when number of jobs are too many (which is the real case).
jenkinsList = (msg) ->
    url = process.env.HUBOT_JENKINS_URL
    filter = new RegExp(msg.match[2], 'i')
    req = msg.http("#{url}/api/json")

    if !msg.match[2]
        msg.reply "```Try with at least 3 charaters.```"
    else
      if msg.match[2].length < 3 
        msg.reply "```Try with at least 3 charaters.```"
      else 
        if process.env.HUBOT_JENKINS_AUTH
          fs = require('fs');
          file = fs.readFileSync(process.env.HUBOT_JENKINS_AUTH, "utf8")
          auth = file.toString()
          req.headers Authorization: "Bearer #{auth}"

        req.get() (err, res, body) ->
          response = ""
          if err
            msg.send "Jenkins says 7: #{err}"
          else
            try
              content = JSON.parse(body)
              for job in content.jobs
                # org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" need to add this too
                if job._class != "com.cloudbees.hudson.plugins.folder.Folder"
                  index = jobList.indexOf(job.name)
                  if index == -1
                    jobList.push(job.name)
                    index = jobList.indexOf(job.name)
                  state = if job.color == "red"
                            "FAIL"
                          else if job.color == "aborted"
                            "ABORTED"
                          else if job.color == "aborted_anime"
                            "CURRENTLY RUNNING"
                          else if job.color == "red_anime"
                            "CURRENTLY RUNNING"
                          else if job.color == "blue_anime"
                            "CURRENTLY RUNNING"
                          else "PASS"
                  if (filter.test job.name) or (filter.test state)
                    response += "*#{state}* #{job.name} #{job.url}\n"
                else 
                  if (filter.test job.name) or (filter.test state)
                    response += "FOLDER #{job.name} #{job.url}\n"
              msg.send response
            catch error
              msg.send error

module.exports = (robot) ->
  robot.respond /j(?:enkins)? build (.*)?/i, (msg) ->
    jenkinsBuild(msg, false)

  robot.respond /j(?:enkins)? proceed (.*)?/i, (msg) ->
    jenkinsProceed(msg, false)

  robot.respond /j(?:enkins)? abort (.*)?/i, (msg) ->
    jenkinsAbort(msg, false)

  robot.respond /j(?:enkins)? list( (.+))?/i, (msg) ->
    jenkinsList(msg)

  robot.respond /j(?:enkins)? desc (.*)?/i, (msg) ->
    jenkinsDescribe(msg)

  robot.respond /j(?:enkins)? last (.*)?/i, (msg) ->
    jenkinsLast(msg)

  robot.jenkins = {
    list: jenkinsList,
    build: jenkinsBuild
    desc: jenkinsDescribe
    last: jenkinsLast
  }
