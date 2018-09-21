# Reload
A Bukkit plugin that customize server's restart behavior. Public under GPLv2.

## Command
- /uptime
  - This command print "2017-12-07 up 33 min(s), 39920 tick(s); Load avg: 20.0, 20.0, 20.0".
- /at
  - This command schedule command to run at today or tomorrow's given clock.
  - Syntax like "/at 12:00 say hi", "/at 2018-11-11T00:01 say hi" or "/at +1h say hi".
  - Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /every
  - This command schedule command to run at everyday(s)'s given clock.
  - Syntax like "/every 12:00 say hello"
  - or or "/every 1h say hello". Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /atq
  - This command manage all scheduled task.
- /shutdown
  - Kick all player(s) to other hub defined in `config.yml` and stop the server.
- /halt
  - Kill the server immediately without data save and cleanup.
  - This command call `taskkill /f` if server in windows, other wise `kill -9`.
- /sudo
  - The "Switch user do" just like linux's.
  - Syntax like "/sudo him say hahaha".

## Expression
Run expression in config.yml to known when to shutdown. If you want to disable it, set it to null or empty string. 

### Expression variable
| variable | description |
|----------|-------------|
| flow     | Count player join and quit  |
| join     | Count player joined         |
| login    | Count player try login      |
| memory   | Ratio of memory usage(0-1)  |
| online   | Current online-player count |
| time     | Server run time by second   |
| tps      | Latest 1 minute tps(0-20)   |
