package shared

import "time"

var startTime = time.Now().Unix()

func Uptime() int64 { return time.Now().Unix() - startTime }
