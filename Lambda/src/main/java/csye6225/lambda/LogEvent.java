package csye6225.lambda;

import java.util.UUID;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

	private DynamoDB dynamo;
	private final String TABLE_NAME = "csye6225";
	private Regions REGION = Regions.US_EAST_1;
	static final String FROM = System.getenv("fromaddr");
	static final String SUBJECT = "Reset Password Link";
	private String body;

	@Override
	public Object handleRequest(SNSEvent request, Context context) {

		LambdaLogger logger = context.getLogger();
		if (request.getRecords() == null) {
			logger.log("No records found!");
			return null;
		}

		logger.log("Email= " + request.getRecords().get(0).getSNS().getMessage());

		logger.log("SNS event=" + request);
		logger.log("Context=" + context);

		String userName = request.getRecords().get(0).getSNS().getMessage();
		String token = UUID.randomUUID().toString();
		this.initDynamoDbClient();
		Item existUser = this.dynamo.getTable(TABLE_NAME).getItem("id", userName);

		if (existUser == null) {
			this.dynamo.getTable(TABLE_NAME).putItem(new PutItemSpec()
					.withItem(new Item().withString("id", userName).withString("Token", token).withLong("TTl", 1200)));
			this.body = "Password reset link here";
		} else {
			this.body = "Password reset link aready sent";
		}
		try {
			Content subject = new Content().withData(SUBJECT);
			Content textbody = new Content().withData(body);
			Body body = new Body().withText(textbody);
			Message message = new Message().withSubject(subject).withBody(body);
			SendEmailRequest emailRequest = new SendEmailRequest()
					.withDestination(new Destination().withToAddresses(userName)).withMessage(message).withSource(FROM);
			AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
					.withRegion(Regions.US_EAST_1).build();
			client.sendEmail(emailRequest);
			logger.log("Email sent successfully!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private void initDynamoDbClient() {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient();
		client.setRegion(Region.getRegion(REGION));
		this.dynamo = new DynamoDB(client);
	}
}
