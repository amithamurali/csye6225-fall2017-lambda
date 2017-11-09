import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

  private DynamoDB dynamoDb;
  private String DYNAMODB_TABLE_NAME = "csye6225";
  private Regions REGION = Regions.US_EAST_1;
  static final String FROM = "noreply@csye6225-fall2017-muralia.me";
  static final String SUBJECT = "Forgot password reset link";
  static final String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
          + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
          + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>"
          + "AWS SDK for Java</a>";

  private String textBody;

  public Object handleRequest(SNSEvent request, Context context) {
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    //Loggers
    context.getLogger().log("Invocation started: " + timeStamp);
    context.getLogger().log("1: " + (request == null));
    context.getLogger().log("2: " + (request.getRecords().size()));
    context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
    timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    context.getLogger().log("Invocation completed: " + timeStamp);

    context.getLogger().log("step 1");
    //Execution
    String username = request.getRecords().get(0).getSNS().getMessage();
    String token = UUID.randomUUID().toString();
   this.initDynamoDbClient();
   this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
            .putItem(
                    new PutItemSpec().withItem(new Item()
                            .withString("userId", username)
                            .withString("token", token)
                            .withInt("TTL", 1800)));
    textBody = "https://csye6225-fall2017.com/reset?email="+username+"&token="+token;

    context.getLogger().log("step 2");
    try {
      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
              .withRegion(Regions.US_EAST_1).build();
      context.getLogger().log("step 3");
      SendEmailRequest emailRequest = new SendEmailRequest()
              .withDestination(
                      new Destination().withToAddresses(username))
              .withMessage(new Message()
                      .withBody(new Body()
                              .withHtml(new Content()
                                      .withCharset("UTF-8").withData(HTMLBODY))
                              .withText(new Content()
                                      .withCharset("UTF-8").withData(textBody)))
                      .withSubject(new Content()
                              .withCharset("UTF-8").withData(SUBJECT)))
              .withSource(FROM);
      client.sendEmail(emailRequest);
      context.getLogger().log("step 4");
      System.out.println("Email sent!");
    } catch (Exception ex) {
      System.out.println("The email was not sent. Error message: "
              + ex.getMessage());
      context.getLogger().log("step 5");
    }
    return null;
  }

  private void initDynamoDbClient() {
    AmazonDynamoDBClient client = new AmazonDynamoDBClient(new BasicAWSCredentials("AKIAIUZLVF7ZPZTBUC5Q","HLIMTAbH4xmGjmse1z+GSsn0bxHX4V9HrNCFurh5"));
    client.setRegion(Region.getRegion(REGION));
    this.dynamoDb = new DynamoDB(client);
  }


}
