package com.tilofy.test.imagecrawler;

import static com.tilofy.test.imagecrawler.ImageCrawlerApplication.JOB_STATUS;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.tilofy.test.imagecrawler.ImageCrawlerApplication.JobStatus;
import com.tilofy.test.imagecrawler.messages.ImageCrawlerActorJob;

public class ImageCrawlActor extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props mkProps() {
        return Props.create(ImageCrawlActor.class);
    }

    @Override
    public void preStart() {
        log.debug("starting actor " + this);
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof ImageCrawlerActorJob) {
            ImageCrawlerActorJob job = (ImageCrawlerActorJob) message;

            log.debug("Job " + job.getJobId() + ": request for \"" + job.getUrl() + "\" at resolution "
                    + job.getWidth() + "x" + job.getHeight());

            BufferedImage resizedImage = getResizedImage(job.getJobId(), job.getUrl(), job.getWidth(), job.getHeight());

            if (resizedImage != null) {
                log.debug("Job " + job.getJobId() + " completed");

                String ext = FilenameUtils.getExtension(new URIBuilder(job.getUrl()).getPath());

                JOB_STATUS.put(job.getJobId(), new JobStatus(resizedImage, "", ext));
            } else {
                log.debug("Job " + job.getJobId() + " failed");

                JOB_STATUS.put(job.getJobId(), new JobStatus(null, "Error processing image", null));
            }

        } else {
            unhandled(message);
        }

    }

    public BufferedImage getResizedImage(int jobId, String url, int width, int height) throws IOException,
            URISyntaxException {

        // TODO: leverage connection pooling to be shared amoust multiple actors
        CloseableHttpClient client = HttpClients.createDefault();

        try {

            InputStream is = downloadFile(url, client);

            if (is == null) {
                return null;
            }

            return createResizedCopy(ImageIO.read(is), width, height, true);

        } finally {

            // TODO: do the HttpGet and Response objects need to be techinically
            // closed as well?
            client.close();
        }
    }

    public static InputStream downloadFile(String url, CloseableHttpClient client) throws IOException {

        HttpGet httpget = new HttpGet(url);
        CloseableHttpResponse response = client.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            return null;
        }
        HttpEntity entity = response.getEntity();
        return entity.getContent();

    }

    public static BufferedImage createResizedCopy(BufferedImage originalImage, int scaledWidth, int scaledHeight,
        boolean preserveAlpha) {

        // TODO: This library doesn't seem to work well with PNGs on my linux
        // machine. Must investigate to find a better alternative
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;

    }

}