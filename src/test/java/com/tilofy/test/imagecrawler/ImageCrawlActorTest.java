package com.tilofy.test.imagecrawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

public class ImageCrawlActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void ensureCanGetResizedImage() throws IOException, URISyntaxException {

        // given
        Props props = ImageCrawlActor.mkProps();
        TestActorRef<ImageCrawlActor> ref = TestActorRef.create(system, props, "imagecrawlerrouter");
        ImageCrawlActor actor = ref.underlyingActor();
        int jobId = 1;
        String url = "http://www.prconversations.com/wp-content/uploads/2011/08/twitter_icon4.jpg";
        int width = 80;
        int height = 60;

        // when
        BufferedImage img = actor.getResizedImage(jobId, url, width, height);

        // then
        assertNotNull(img);

    }

    @Test
    public void ensureCanDownloadFile() throws IOException, URISyntaxException {

        // given
        // TODO: Find a better url to use as a test
        String url = "http://www.atmos.org/CNAME";
        CloseableHttpClient client = HttpClients.createDefault();
        String actual = null;

        // when
        try {
            InputStream is = ImageCrawlActor.downloadFile(url, client);
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, Charset.forName("utf8"));
            actual = writer.toString();
        } finally {
            client.close();
        }

        // then
        assertEquals("www.atmos.org\n", actual);

    }

    @Test
    public void ensureCanResize() throws IOException {

        // given
        BufferedImage img = ImageIO.read(new File("src/test/resources/unmangler.jpg"));

        // when
        BufferedImage actual = ImageCrawlActor.createResizedCopy(img, 10, 20, true);

        // then
        assertEquals(10, actual.getWidth());
        assertEquals(20, actual.getHeight());

    }
}
