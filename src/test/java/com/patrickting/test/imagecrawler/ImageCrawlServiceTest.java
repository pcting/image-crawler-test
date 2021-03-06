package com.patrickting.test.imagecrawler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import org.junit.Test;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

import com.patrickting.test.imagecrawler.ImageCrawlerService;
import com.patrickting.test.imagecrawler.model.EnqueueJobResponse;

public class ImageCrawlServiceTest {

    @Test
    public void ensureCanGetResizedImage() throws IOException, URISyntaxException {

        // given
        ActorSelection actor = mock(ActorSelection.class);
        ActorSystem system = mock(ActorSystem.class);
        ImageCrawlerService service = spy(new ImageCrawlerService(system));
        String url = "https://raw.github.com/pcting/image-crawler-test/master/src/test/resources/unmangler.jpg";
        String size = "80x60";
        int jobId = 12345;
        when(service.getNextJobId()).thenReturn(jobId);
        when(system.actorSelection("/user/imagecrawlerrouter")).thenReturn(actor);

        // when
        Response resp = service.queueJob(url, size);

        // then
        assertEquals(200, resp.getStatus());
        assertEquals(jobId, ((EnqueueJobResponse) resp.getEntity()).getJobId());

    }
}
