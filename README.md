How to Run
==========

Starting the server:

        mvn tomcat7:run


Testing with curl:

        for i in {1..20}; do curl -H   "Accept: application/json" -H "Content-type: application/json" -X POST 'http://localhost:9090/queue/?url=http%3A//www.prconversations.com/wp-content/uploads/2011/08/twitter_icon4.jpg&size=800x600'; done

Reading downloading the scaled image:

        curl http://localhost:9090/queue/1 > image.jpg
