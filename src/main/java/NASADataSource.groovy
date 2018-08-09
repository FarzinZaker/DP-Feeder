/**
 * Created by root on 8/15/17.
 */
class NASADataSource {

    private static Date lastDate

    public static void feed(Integer speed) {
        def fileName = "NASA_access_log_Jul95"
        def file = new File(fileName)

        def commands = file.readLines().collect { String command ->
            def parts = command.split('\"')
            if (parts.size() > 2) {
                def urlParts = parts[1]?.trim()?.split(' ')
                if (urlParts.size() > 1) {
                    [
                            date        : Date.parse('dd/MMM/yyyy:hh:mm:ss', parts[0]?.trim()?.split('\\[')?.last()?.split(' ')?.first()),
                            path        : parts[1]?.trim()?.split(' ')[1],
                            responseTime: parseInt(parts[2]?.trim()?.split(' ')?.last())
                    ]
                } else null
            } else null
        }?.findAll { it }?.sort { it.date }
        commands.each { command ->
            if (!lastDate)
                lastDate = command.date
            def delay = Math.round((command.date.time - lastDate.time) / speed)
            println delay
            Thread.sleep(delay)
            sendRequest(command.path)
            lastDate = command.date
        }
    }

    private static void sendRequest(String path) {
        println path
    }

    private static Integer parseInt(String input) {
        try {
            input?.toInteger()
        } catch (ignored) {
            0
        }
    }
}
