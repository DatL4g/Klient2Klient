import de.datlag.k2k.discover.discovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test

class DiscoveryTest {

    @Test
    fun createInstance() {
        val discovery = CoroutineScope(Dispatchers.IO).discovery {
            setDiscoveryTimeout(20000)
            setDiscoverableTimeout(20000)
            setPing(1000)
            setPuffer(3000)
            setPort(1337)
            setDiscoveryTimeoutListener { }
        }
        discovery.makeDiscoverable("Test")
        discovery.startDiscovery()
    }
}