package com.IceCreamQAQ.Yu.cache

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Default
import com.IceCreamQAQ.Yu.annotation.NotSearch
import com.IceCreamQAQ.Yu.di.BeanFactory
import com.IceCreamQAQ.Yu.di.ConfigManager
import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import net.sf.ehcache.Element
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@NotSearch
class EhcacheHelp<T>(private val cache: Cache) {

    operator fun get(key: String): T? {
        return cache.get(key)?.objectValue as? T?
    }

    operator fun set(key: String, value: T) {
        cache.put(Element(key, value))
    }

    fun remove(key: String) {
        cache.remove(key)
    }

}

class EhcacheHelpFactory : BeanFactory<EhcacheHelp<*>?>, ApplicationService {

    @Config("yu.cache.ehcache.config")
    @Default("ehcache-yu-default.xml")
    private lateinit var ehcacheConfigLocation: String
    private var cm: CacheManager? = null

    @Inject
    private lateinit var configManager: ConfigManager

    private var cmDefaultMap = ConcurrentHashMap<String, CacheManager>()

    override fun createBean(clazz: Class<EhcacheHelp<*>?>, name: String): EhcacheHelp<*>? {
        val cache = cm?.getCache(name) ?: {
            val cm = cmDefaultMap[name] ?: {
                val location = configManager.get("yu.cache.ehcache.caches.$name.default", String::class.java)
                if (location != null) {
                    val url = javaClass.classLoader.getResource(location)
                    if (url != null) {
                        val cm = CacheManager.newInstance(url) ?: null
                        if (cm != null) cmDefaultMap[name] = cm
                        cm
                    }
                    else null
                } else null
            }()
            cm?.getCache(name)
        }() ?: return null
        return EhcacheHelp<Any>(cache)
    }

    override fun init() {
        val url = javaClass.classLoader.getResource(ehcacheConfigLocation) ?: return
        cm = CacheManager.newInstance(url)
    }

    override fun start() {

    }

    override fun stop() {
        cm?.shutdown()
        for (cache in cmDefaultMap.values) {
            cache.shutdown()
        }
    }
}
