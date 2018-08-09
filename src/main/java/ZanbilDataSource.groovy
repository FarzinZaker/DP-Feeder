import groovy.time.TimeCategory
import groovy.time.TimeDuration

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ZanbilDataSource {

    static String base = '/home/ubuntu/'
//    static String base = '/Personal/'

    static void feed(Integer speed) {
        aggregateRequests(speed)

        def scenarios = ['basket', 'product', 'browse', 'static']
        def threads = []
        scenarios.each { scenario ->
            threads << Thread.start {
                feedScenario(scenario, speed)
            }
        }
        threads.each { Thread thread -> thread.join() }

        println 'FINISHED !'
    }

    static void feed(String scenario, Integer speed, Integer step, String inet, String proxyAddress) {

        def start = new Date()
        aggregateRequests(speed)

        def file = new File("${base}AccessLogs/Aggregated/${scenario}-${speed}")
        def threadPool = Executors.newFixedThreadPool(200)
//        try {
        List<Future> futures = (1..Math.round(file.readLines().get(step)?.trim()?.toInteger() / 10))?.collect { num ->
            threadPool.submit({ ->
                sendRequest(scenario, inet, proxyAddress)
            } as Callable)
        }
        futures.each { it.get() }
//        } finally {
        threadPool.shutdown()
//        }
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

//        def threads = []
//        file.readLines().get(step)?.trim()?.toInteger()?.times {
//            threads << sendRequest(scenario, inet, proxyAddress)
//        }
//        threads.each { Thread thread -> thread.start() }
//        println Thread.activeCount()
//        threads.each { Thread thread -> thread.join() }
        println (new Date().time - start.time)
    }

    private static void feedScenario(String scenario, Integer speed) {

        def file = new File("${base}AccessLogs/Aggregated/${scenario}-${speed}")

        file.eachLine { line ->
            def threads = []
            line?.toInteger()?.times {
                threads << sendRequest(scenario)
            }
//            threads.each { Thread thread -> thread.join() }
            Thread.sleep(1000)
        }
    }

//    private static Thread sendRequest(String scenario, String inet, String proxyAddress) {
    private static void sendRequest(String scenario, String inet, String proxyAddress) {
        def url = "http://${proxyAddress}/${scenario}"
        def responseTimeReportUrl = "http://${proxyAddress}/rt/${scenario}"
//        def thread = new Thread() {
//            void run() {
        try {
            def startTime = new Date()

            def proc = Runtime.getRuntime().exec("curl${inet != 'local' ? " --interface ${inet}" : ''} ${url}")
            def sout = new StringBuffer()
            def serr = new StringBuffer()

            proc.consumeProcessOutput(sout, serr)
            proc.waitFor()

            def responseTime = new Date().time - startTime.time

            println "${scenario} -> ${responseTime}"

//            Thread.start {
            try {
                new URL(responseTimeReportUrl + (url.contains('?') ? '&' : '?') + 'rt=' + Math.round(responseTime)?.toInteger()).text
            } catch (exception) {
                println(responseTimeReportUrl + (url.contains('?') ? '&' : '?') + 'rt=' + Math.round(responseTime)?.toInteger())
                println("RT: " + exception.message)
            }
//            }
        }

        catch (exception) {
            println("OR:" + exception.message)
        }
//            }
//        }

//        thread
    }

    private static Integer parseInt(String input) {
        try {
            input?.toInteger()
        } catch (ignored) {
            0
        }
    }


    private static void aggregateRequests(Integer speed) {
        def scenarios = ['basket', 'product', 'browse', 'static']
        def fileName = "${base}AccessLogs/Normalize/[scenario]"
        def correctedFileName = "${base}AccessLogs/Aggregated/[scenario]-${speed}"
        scenarios.each { scenario ->
//            println correctedFileName?.replace('[scenario]', scenario)
            def correctedFile = new File(correctedFileName?.replace('[scenario]', scenario))
            if (correctedFile.exists())
                return
//            else
//                println "file does not exist"

            correctedFile.createNewFile()
            def file = new File(fileName?.replace('[scenario]', scenario))

            def totalCounter = 3 * 60 * 60
            def counter = 0
            def sum = 0
            file.eachLine { line ->
                if (totalCounter > 0) {
                    sum += line?.trim()?.toInteger()
                    counter++
                    if (counter == speed || totalCounter == 0) {
                        totalCounter--
                        correctedFile.append("${sum}\n")
                        counter = 0
                        sum = 0
                    }
                }
            }
        }
    }

    private static void normalizeWaitTimes() {
        def scenarios = ['basket', 'product', 'browse', 'static']
        def fileName = "${base}AccessLogs/WaitTimes/[scenario]"
        def correctedFileName = "${base}AccessLogs/Normalize/[scenario]"
        scenarios.each { scenario ->
            def file = new File(fileName?.replace('[scenario]', scenario))
            def correctedFile = new File(correctedFileName?.replace('[scenario]', scenario))
            if (!correctedFile.exists())
                correctedFile.createNewFile()

            def remaining = 0
            file.eachLine { line ->
                def delay = line?.trim()?.toInteger()
                if (delay == 0) {
                    remaining++
                } else {
                    def command = ''
                    delay.times {
                        command += '0\n'
                    }
                    command += "${remaining + 1}\n"
                    remaining = 0
                    correctedFile.append(command)
                }
            }
        }
    }

    private static void convertDatesToWaitTime() {
        def scenarios = ['basket', 'product', 'browse', 'static']
        def fileName = "${base}AccessLogs/[scenario]"
        def correctedFileName = "${base}AccessLogs/WaitTimes/[scenario]"
        scenarios.each { scenario ->
            def file = new File(fileName?.replace('[scenario]', scenario))
            def correctedFile = new File(correctedFileName?.replace('[scenario]', scenario))
            if (!correctedFile.exists())
                correctedFile.createNewFile()

            def previousDate
            file.eachLine { line ->
                def date = Date.parse('dd/MMM/yyyy:hh:mm:ss', line)
                if (!previousDate)
                    previousDate = date
                TimeDuration td = TimeCategory.minus(date as Date, previousDate as Date)
                correctedFile.append(td.seconds + '\n')
                previousDate = date
            }
        }
    }

    private static void prepareFiles() {
        def scenarios = ['static', 'filter', 'browse', 'product', 'search', 'orderAdministration', 'basket']
        def scenarioTranslation = [
                image              : 'image',
                static             : 'static',
                filter             : 'browse',
                browse             : 'browse',
                search             : 'browse',
                product            : 'product',
                basket             : 'basket',
                orderAdministration: 'basket'
        ]
        def fileName = "${base}AccessLogs/access.log"
        def correctedFileName = "${base}AccessLogs/[scenario]"
        def file = new File(fileName)

        file.eachLine { String line, Integer index ->
            def parts = line.split('\"')
            if (parts.size() > 2) {
                def urlParts = parts[1]?.trim()?.split(' ')
                if (urlParts.size() > 1) {
                    def command = [
                            date        : Date.parse('dd/MMM/yyyy:hh:mm:ss', parts[0]?.trim()?.split('\\[')?.last()?.split(' ')?.first()),
                            path        : parts[1]?.trim()?.split(' ')[1],
                            responseTime: parseInt(parts[2]?.trim()?.split(' ')?.last())
                    ]

                    def scenario = command.path?.split('/')?.findAll { it }?.find()?.split('\\?')?.find()
                    if (scenario == 'm')
                        try {
                            scenario = command.path?.split('/')?.findAll { it }[1]?.split('\\?')?.find()
                        } catch (ignored) {
                        }
                    if (scenario && scenarios.contains(scenario)) {
                        def correctedFile = new File(correctedFileName.replace('[scenario]', scenarioTranslation[scenario]))
                        if (!correctedFile.exists())
                            correctedFile.createNewFile()
                        correctedFile.append((command.date as Date)?.format('dd/MMM/yyyy:hh:mm:ss') + '\n')
                    }

                } else null
            } else null
        }
    }

}
