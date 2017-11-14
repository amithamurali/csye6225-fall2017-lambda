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
  static String token;
  static String username;
  static final String FROM = "noreply@csye6225-fall2017-muralia.me";
  static final String SUBJECT = "Forgot password reset link";
  static String HTMLBODY;

  private static String TEXTBODY;


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
    username = request.getRecords().get(0).getSNS().getMessage();
    context.getLogger().log( username );
    token = UUID.randomUUID().toString();
    context.getLogger().log( token );
      
   this.initDynamoDbClient(context);

      if ((this.dynamoDb.getTable( DYNAMODB_TABLE_NAME ).getItem( "userId", username )) == null) {

          context.getLogger().log("user does not in the dynamo db table, create new token and send email");
          this.dynamoDb.getTable( DYNAMODB_TABLE_NAME )
                  .putItem(
                          new PutItemSpec().withItem( new Item()
                                  .withString( "userId", username )
                                  .withString( "token", token )
                                  .withInt( "TTL", 1200 ) ) );

          TEXTBODY = "https://csye6225-fall2017.com/reset?email=" + username + "&token=" + token;
          context.getLogger().log( "This is text body: " + TEXTBODY );
          HTMLBODY = "<h2>You have successfully sent an Email using Amazon SES!</h2>"
                  + "<p>Please reset the password using the below link. " +
                  "Link: https://csye6225-fall2017.com/reset?email=" + username + "&token=" + token+"</p>";
          context.getLogger().log( "This is HTML body: " + HTMLBODY );

          //final String TEXTBODY = textBody;

          context.getLogger().log( "step 2" );
          try {
              AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                      .withRegion( Regions.US_EAST_1 ).build();
              context.getLogger().log( "step 3" );
              SendEmailRequest emailRequest = new SendEmailRequest()
                      .withDestination(
                              new Destination().withToAddresses( username ) )
                      .withMessage( new Message()
                              .withBody( new Body()
                                      .withHtml( new Content()
                                              .withCharset( "UTF-8" ).withData( HTMLBODY ) )
                                      .withText( new Content()
                                              .withCharset( "UTF-8" ).withData( TEXTBODY ) ) )
                              .withSubject( new Content()
                                      .withCharset( "UTF-8" ).withData( SUBJECT ) ) )
                      .withSource( FROM );
              client.sendEmail( emailRequest );
              context.getLogger().log( "step 4" );
              System.out.println( "Email sent!" );
          } catch (Exception ex) {
              System.out.println( "The email was not sent. Error message: "
                      + ex.getMessage() );
              context.getLogger().log( "step 5" );
          }

      }
      else
      {
          context.getLogger().log("user exists in the dynamo db table");
      }

    return null;
  }

  private void initDynamoDbClient(Context context) {
    String accessKey = System.getenv("accessKey");
    String secretKey = System.getenv("secretKey");
    
    AmazonDynamoDBClient client = new AmazonDynamoDBClient(new BasicAWSCredentials(accessKey,secretKey));
    context.getLogger().log(client.toString());
    client.setRegion(Region.getRegion(REGION));
    this.dynamoDb = new DynamoDB(client);
  }


}
