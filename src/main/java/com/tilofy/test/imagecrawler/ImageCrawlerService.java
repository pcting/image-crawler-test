package com.tilofy.test.imagecrawler;

import static com.tilofy.test.imagecrawler.ImageCrawlerApplication.JOB_STATUS;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.tilofy.test.imagecrawler.ImageCrawlerApplication.JobStatus;
import com.tilofy.test.imagecrawler.messages.ImageCrawlerActorJob;
import com.tilofy.test.imagecrawler.model.EnqueueJobResponse;

@Path("/queue/")
public class ImageCrawlerService {

    @Context private ActorSystem actorSystem;

    public ImageCrawlerService() {
    }

    // TODO: remove method; this is only needed for testing
    public ImageCrawlerService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    // TODO: remove method; this is only needed for testing
    public int getNextJobId() {
        return ImageCrawlerApplication.JOB_ID_COUNTER.incrementAndGet();
    }

    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+)x([0-9]+)");

    private Logger log = LoggerFactory.getLogger(ImageCrawlerService.class);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response queueJob(@QueryParam("url") String url, @QueryParam("size") String size) {

        Matcher dims = SIZE_PATTERN.matcher(size);

        if (!dims.find()) {
            log.info("Invalid size parsed: " + size);
            return Response.status(400).build();
        }

        int width = Integer.parseInt(dims.group(1));

        int height = Integer.parseInt(dims.group(2));

        int jobId = getNextJobId();

        ActorSelection actor = actorSystem.actorSelection("/user/imagecrawlerrouter");

        Timeout timeout = new Timeout(Duration.create(30, "seconds"));

        Patterns.ask(actor, new ImageCrawlerActorJob(jobId, url, width, height), timeout);

        return Response.ok(new EnqueueJobResponse(jobId)).build();

    }

    @Path("{jobid}")
    @Produces({ "image/png", "image/jpeg", "image/gif", "text/plain" })
    @GET
    public Response getJobId(@PathParam("jobid") Integer jobId) {
        final JobStatus status = JOB_STATUS.getIfPresent(jobId);

        if (status == null) {
            return Response.status(404).entity("").build();
        }

        if (status.image == null) {
            Map<String, String> response = new HashMap<String, String>();
            response.put("error", status.message);
            return Response.status(412).entity(response).type(MediaType.APPLICATION_JSON).build();
        }

        StreamingOutput out = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                try {
                    ImageIO.write(status.image, status.extension, out);
                } catch (IOException e) {
                    log.error("Error writing out image", e);
                }

            }
        };

        return Response.ok(out).build();
    }
}