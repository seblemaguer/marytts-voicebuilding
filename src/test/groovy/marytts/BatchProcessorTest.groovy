package marytts

import groovy.json.JsonBuilder
import groovy.util.logging.Log4j

import org.custommonkey.xmlunit.*

import org.testng.annotations.*

@Log4j
class BatchProcessorTest {

    def tmpDir
    def examples = ['example1', 'example2']

    @BeforeSuite
    void setUp() {
        // unpack text resources
        tmpDir = File.createTempDir()
        log.info "tmpDir = $tmpDir"
        examples.each { example ->
            def exampleFile = new File("$tmpDir/text", "${example}.text")
            exampleFile.parentFile.mkdirs()
            exampleFile.withOutputStream { stream ->
                stream << getClass().getResourceAsStream("${example}.text")
            }
        }
        XMLUnit.ignoreWhitespace = true
    }

    @Test
    void processTextToTokens() {
        // create batch
        def batch = examples.collect { example ->
            def inputPath = "$tmpDir/text/${example}.text"
            def outputPath = inputPath.replaceAll('text', 'tokens')
            [
                    locale    : "${Locale.US}",
                    inputType : 'TEXT',
                    outputType: 'TOKENS',
                    inputFile : inputPath,
                    outputFile: outputPath
            ]
        }
        def json = new JsonBuilder(batch).toPrettyString()
        log.info "batch = $json"
        def batchFile = File.createTempFile('batch', '.json')
        log.info "batchFile = $batchFile"
        batchFile.text = json

        // run the batch
        BatchProcessor.main([batchFile.path] as String[]);

        // test the result
        batch.each { request ->
            def expected = getClass().getResourceAsStream(new File(request.outputFile).name).text
            def actual = new File(request.outputFile).text
            def comparison = XMLUnit.compareXML(expected, actual)
            def data = new DetailedDiff(comparison)
            assert data.similar()
        }
    }
}
