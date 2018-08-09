import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by root on 8/14/17.
 */
class DataSource {

    static String carDataFileName = "REAL_DATA.txt"
    static Path carDataFilePath = Paths.get(carDataFileName)
    static Integer requestsRounds = 2000
    static Integer maxRequestsPerScenario = 50

    ExecutorService threadPool


    private def random = new Random()

    private Integer random(Range<Integer> range) {
        Integer result = -1
        while (result < 0 || result < range?.first() || result > range?.last()) {
            Double rand = random.nextGaussian()
            result = Math.round((rand + 2) * range.size() / 4 + range?.first()) as Integer
        }
        result
    }

    DataSource(Integer scenariosCount) {
        //create data file
        if (!Files.exists(carDataFilePath)) {
            Files.createFile(carDataFilePath)
            def writer = new PrintWriter(new File(carDataFileName))
            def period = random(5..20)
            requestsRounds.times {
                writer.write("""${
                    (0..scenariosCount).collect {
                        random(0..maxRequestsPerScenario)
                    }?.join(" ")
                }\r\n""")

                period -= 1
                if (period == 0) {
                    writer.write("sleep ${random(100..300)}\r\n")
                    period = random(5..20)
                }
            }
            writer.close()
        }

        threadPool = Executors.newFixedThreadPool(4)

    }

    void feed(String host, List<String> scenarioPaths) {
        def file = new File(carDataFileName)
        file.readLines().each { String command ->
            println command
            if (command?.startsWith('sleep'))
                Thread.sleep(command?.trim()?.split(" ")?.last()?.toLong())
            else {
                def counts = command?.trim()?.split(" ")
                for (def i = 0; i < [counts?.size(), scenarioPaths?.size()]?.min(); i++) {
                    sendRequests(host + scenarioPaths[i], counts[i]?.toInteger())
                }
            }
        }
    }

    void sendRequests(String url, Integer count) {
        count.times {
            threadPool.submit {
                println new URL(url).text
            }
        }
    }
}
