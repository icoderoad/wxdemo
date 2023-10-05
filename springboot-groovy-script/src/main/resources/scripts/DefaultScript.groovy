// DefaultScript.groovy
import java.text.SimpleDateFormat
import java.util.Date

def getCurrentTime() {
    def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return dateFormat.format(new Date())
}

def getCurrentDayOfWeek() {
    def dateFormat = new SimpleDateFormat("EEEE")
    return dateFormat.format(new Date())
}