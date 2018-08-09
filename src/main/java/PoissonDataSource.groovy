import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by root on 8/15/17.
 */
class PoissonDataSource {

    static List<Integer> numberOfUsersPerRound = [250, 220, 350, 650, 750]
    static Integer lengthOfEachRound = 100

    List<Map> config

    PoissonDataSource(List<Map> config, Integer index) {
        this.config = config
        if (index != null)
            generateFeedData(config.find { it.index == index })
        else {
            config.each { scenario ->
                generateFeedData(scenario)
            }
        }
    }

    private static void generateFeedData(Map scenario) {
        def fileName = "POISSON_DATA_${scenario.index}.txt"
        def filePath = Paths.get(fileName)
        if (!Files.exists(filePath)) {
            Files.createFile(filePath)
//            def random = new Random()
            def writer = new PrintWriter(new File(fileName))
            def roundIndexer = 0
            def mean = lengthOfEachRound / 2

            //variance
            long n = 0;
            double m = 0;
            double s = 0.0;
            def population = 1..lengthOfEachRound
            population.each {
                n++;
                double delta = it - m
                m += delta / n;
                s += delta * (it - m)
            }
            def variance = s / n

            numberOfUsersPerRound.each { numberOfUsers ->
                def average = (scenario.weight as Float) * numberOfUsers
                def data = [:]
                def stdDeviation = 0.1
                (lengthOfEachRound / 2).times { x ->
//                    def value = Math.round(it * average * 2 / lengthOfEachRound)?.toInteger()

//                    def value = Math.round((1 / (numberOfUsers * Math.sqrt(2 * 3 * 14.159))) *
//                            Math.pow(2.71828, -(Math.pow(it - (lengthOfEachRound / 2), 2) / (2 * Math.pow(numberOfUsers, 2))))).toInteger()

                    def value = Math.round(Math.pow(Math.exp(-(((x - mean) * (x - mean)) / ((2 * variance)))), 1 / (stdDeviation * Math.sqrt(2 * Math.PI))) * average).toInteger()

                    data.put(x, value)
                    data.put(lengthOfEachRound - x, value)
                }
//                data.putIfAbsent(Math.round(lengthOfEachRound / 2).toInteger(), Math.round(average).toInteger())
                data = data.sort { it.key }
                data.each {
                    writer.write("${roundIndexer * 100 + it.key} ${it.value}\r\n")
                }
                roundIndexer++
            }
            writer.close()
        }
    }

    void feed(Integer speed, String sourceIP, Integer index, String host, Integer step) {
        if (index != null) {
            def scenario = config[index]
            feed(speed, sourceIP, "${host}${scenario.path}", "${host}rt/${scenario.path}", scenario.index as Integer, step)
        } else {
            def threads = new ArrayList<Thread>()
            config.each { scenario ->
                threads.add(new Thread() {
                    void run() {
                        feed(speed, sourceIP, "${host}${scenario.path}", "${host}rt/${scenario.path}", scenario.index as Integer, step)
                    }
                })
            }
            threads.each { it.start() }
            threads.each { it.join() }
        }
    }

    void feed(Integer speed, String sourceIP, String url, String responseTimeReportUrl, Integer index, Integer step) {
        def fileName = "POISSON_DATA_${index}.txt"
        def file = new File(fileName)

        if (step == null)
            file.readLines().each { String command ->
                executeRequest(sourceIP, url, responseTimeReportUrl, command)
                Thread.sleep(speed)
            }
        else {
            def command = file.readLines().find { it.startsWith("${step} ") }
            executeRequest(sourceIP, url, responseTimeReportUrl, command)
        }
    }

    void executeRequest(String sourceIP, String url, String responseTimeReportUrl, String command) {
        def requests = Math.round(command?.split(' ')?.last()?.toInteger())

        def threads = new ArrayList<Thread>()
        requests.times {
            threads.add(new Thread() {
                void run() {
                    try {
                        def startTime = new Date()

                        def proc = Runtime.getRuntime().exec("curl --interface ${sourceIP} ${url}")
                        def sout = new StringBuffer()
                        def serr = new StringBuffer()

                        proc.consumeProcessOutput(sout, serr)
                        proc.waitFor()
//                            println(sout)

                        def responseTime = new Date().time - startTime.time
                        start {
//                                def rtClient = new AsyncHTTPBuilder(poolSize: 1)
//                                rtClient.request(responseTimeReportUrl + (url.contains('?') ? '&' : '?') + 'rt=' + responseTime,
//                                        Method.GET,
//                                        ContentType.TEXT) {
//                                }
                            new URL(responseTimeReportUrl + (url.contains('?') ? '&' : '?') + 'rt=' + Math.round(responseTime)?.toInteger()).text
                        }
                    } catch (exception) {
                        println(exception.message)
                    }
                }
            })
        }
        threads.each {
            it.start()
        }
        threads.each {
            it.join()
        }

        println "DONE"
    }
}
