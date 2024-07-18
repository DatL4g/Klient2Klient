import dev.datlag.k2k.discover.discovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test

class DiscoveryTest {

    @Test
    fun createInstance() {
        val discovery = CoroutineScope(Dispatchers.IO).discovery {
            setSearchTimeout(20000)
            setShowTimeout(20000)
            setPing(1000)
            setPort(1337)
        }
        discovery.show("Test")
        discovery.search()

        discovery.close()
    }
}