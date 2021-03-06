package org.infinispan.persistence.rest;

import org.infinispan.commons.io.ByteBufferFactoryImpl;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.container.InternalEntryFactoryImpl;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.persistence.rest.configuration.RestStoreConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.MarshalledEntryFactoryImpl;
import org.infinispan.persistence.BaseStoreTest;
import org.infinispan.persistence.DummyInitializationContext;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.rest.EmbeddedRestServer;
import org.infinispan.rest.RestTestingUtil;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TestInternalCacheEntryFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Tristan Tarrant
 * @since 6.0
 */
@Test(testName = "persistence.rest.RestStoreTest", groups = "unstable", description = "original group: functional")
public class RestStoreTest extends BaseStoreTest {

   private static final String REMOTE_CACHE = "remote-cache";
   private EmbeddedCacheManager localCacheManager;
   private EmbeddedRestServer restServer;

   @Override
   protected AdvancedLoadWriteStore createStore() throws Exception {
      ConfigurationBuilder cb = TestCacheManagerFactory.getDefaultCacheConfiguration(false);
      cb.eviction().maxEntries(100).strategy(EvictionStrategy.UNORDERED).expiration().wakeUpInterval(10L);

      GlobalConfigurationBuilder globalConfig = new GlobalConfigurationBuilder().nonClusteredDefault();
      globalConfig.globalJmxStatistics().allowDuplicateDomains(true);

      localCacheManager = TestCacheManagerFactory.createCacheManager(globalConfig, cb);
      localCacheManager.getCache(REMOTE_CACHE);
      restServer = RestTestingUtil.startRestServer(localCacheManager);

      RestStoreConfigurationBuilder storeConfigurationBuilder = TestCacheManagerFactory.getDefaultCacheConfiguration(false).persistence()
            .addStore(RestStoreConfigurationBuilder.class);
      storeConfigurationBuilder.host(restServer.getHost()).port(restServer.getPort()).path("/rest/" + REMOTE_CACHE);
      storeConfigurationBuilder.connectionPool().maxTotalConnections(10).maxConnectionsPerHost(10);
      storeConfigurationBuilder.validate();
      RestStore restStore = new RestStore();
      restStore.init(new DummyInitializationContext(storeConfigurationBuilder.create(), getCache(), getMarshaller(),
                                                    new ByteBufferFactoryImpl(), new MarshalledEntryFactoryImpl(getMarshaller())));
      InternalEntryFactoryImpl iceFactory = new InternalEntryFactoryImpl();
      iceFactory.injectTimeService(TIME_SERVICE);
      restStore.setInternalCacheEntryFactory(iceFactory);
      restStore.start();
      return restStore;
   }

   @Override
   @AfterMethod(alwaysRun = true)
   public void tearDown() {
      if (restServer != null) {
         RestTestingUtil.killServers(restServer);
      }
      if (localCacheManager != null) {
         TestingUtil.killCacheManagers(localCacheManager);
      }
   }

   @Override
   protected void assertEventuallyExpires(String key) throws Exception {
      for (int i = 0; i < 10; i++) {
         if (cl.load("k") == null)
            break;
         Thread.sleep(1000);
      }
      assert cl.load("k") == null;
   }

   @Override
   protected void sleepForStopStartTest() throws InterruptedException {
      Thread.sleep(3000);
   }

   @Override
   protected void purgeExpired()  {
      localCacheManager.getCache().getAdvancedCache().getEvictionManager().processEviction();
   }

    /*
    * Unfortunately we need to mark each test individual as unstable because the super class belong to a valid test
    * group. I think that it appends the unstable group to the super class group making it running the tests anyway.
    */

   @Test(groups = "unstable")
   @Override
   public void testReplaceExpiredEntry() throws Exception {
      InternalCacheEntry ice = TestInternalCacheEntryFactory.create("k1", "v1", 100);
      cl.write(TestingUtil.marshalledEntry(ice, getMarshaller()));
      // Hot Rod does not support milliseconds, so 100ms is rounded to the nearest second,
      // and so data is stored for 1 second here. Adjust waiting time accordingly.
      TestingUtil.sleepThread(1100);
      assert null == cl.load("k1");
      InternalCacheEntry ice2 = TestInternalCacheEntryFactory.create("k1", "v2", 100);
      cl.write(TestingUtil.marshalledEntry(ice2, getMarshaller()));
      assert cl.load("k1").getValue().equals("v2");
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAndStoreImmortal() throws PersistenceException {
      super.testLoadAndStoreImmortal();
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAndStoreWithLifespan() throws Exception {
      super.testLoadAndStoreWithLifespan();
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAndStoreWithIdle() throws Exception {
      super.testLoadAndStoreWithIdle();
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAndStoreWithLifespanAndIdle() throws Exception {
      super.testLoadAndStoreWithLifespanAndIdle();
   }

   @Test(groups = "unstable")
   @Override
   public void testStopStartDoesNotNukeValues() throws InterruptedException, PersistenceException {
      super.testStopStartDoesNotNukeValues();
   }

   @Test(groups = "unstable")
   @Override
   public void testPreload() throws Exception {
      super.testPreload();
   }

   @Test(groups = "unstable")
   @Override
   public void testStoreAndRemove() throws PersistenceException {
      super.testStoreAndRemove();
   }

   @Test(groups = "unstable")
   @Override
   public void testPurgeExpired() throws Exception {
      super.testPurgeExpired();
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAll() throws PersistenceException {
      super.testLoadAll();
   }

   @Test(groups = "unstable")
   @Override
   public void testLoadAndStoreMarshalledValues() throws PersistenceException {
      super.testLoadAndStoreMarshalledValues();
   }
}
