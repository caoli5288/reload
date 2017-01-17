# Reload
## Expression
在配置文件`control.expr`路径填入你的表达式，或者布尔值。`true`代表使用默认的`(time > 36000 && online < 1) || tps < 5`表达式，而`false`则代表禁用此功能。

### Variable

| variable | description |
|----------|-------------|
| runtime  | Server runtime by second. |
| online   | Current online-player count. |
| flow     | Count total player joined. |
| load     | Ratio of memory usage(0-1). |
| tps      | Latest 1 minute tps(0-20). |