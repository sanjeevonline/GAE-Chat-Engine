package com.sanjeevonline.schoolcloud;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.xmpp.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@SuppressWarnings("serial")
public class SchoolcloudServlet extends HttpServlet {
	public static final Logger _log = Logger.getLogger(SchoolcloudServlet.class
			.getName());

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String strCallResult = "";
		try {
			String strStatus = "";
			XMPPService xmpp = XMPPServiceFactory.getXMPPService();
			// STEP 2
			Message msg = xmpp.parseMessage(req);
			JID fromJid = msg.getFromJid();
			String body = msg.getBody().toLowerCase().trim();
			_log.info("Received a message from " + fromJid + " and body = "
					+ body);
			// STEP 3
			String serviceUrl = null;
			String xpathExpression = null;
			String responseMessage = null;
			String msgBody = null;
			boolean isError = false;
			String errorMessage = "ERROR: Oops!!! something seems to be wrong with the your request paramenters. The correct usage is \n"
					+ " For Dictionary service, type : D <Your Word> \n"
					+ " For Weather service, type : W <City Name> ";
			if (body.startsWith("d ")) {
				serviceUrl = "http://services.aonaware.com/DictService/DictService.asmx/Define?word=";
				xpathExpression = "//Definition[Dictionary[Id='wn']]/WordDefinition/text()";
				responseMessage = "Dictionary service requested for the word : ";
			} else if (body.startsWith("w ")) {
				serviceUrl = "http://api.wunderground.com/auto/wui/geo/ForecastXML/index.xml?query=";
				xpathExpression = "//txt_forecast/forecastday/fcttext/text()";
				responseMessage = "Weather service requested for the city : ";
			} else {
				isError = true;
			}

			body = body.substring((body.lastIndexOf(" ") + 1));
			serviceUrl += body;

			if (!isError) {
				URL url = new URL(serviceUrl);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(url.openStream()));
				StringBuffer response = new StringBuffer();
				String line;

				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
				reader.close();
				strCallResult = response.toString();

				DocumentBuilderFactory builderFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				Document doc = builder.parse(new InputSource(new StringReader(
						strCallResult.toString())));
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				XPathExpression expr = xpath.compile(xpathExpression);
				Object result = expr.evaluate(doc, XPathConstants.NODESET);
				NodeList nodes = (NodeList) result;
				for (int i = 0; i < nodes.getLength(); i++) {
					strCallResult = nodes.item(i).getNodeValue();
				}
			}
			// resp.getWriter().println(strCallResult);
			if (strCallResult.startsWith("<") || isError) {
				msgBody = errorMessage;
			} else {
				msgBody = responseMessage + body + "\n\n" + strCallResult;
			}
			Message replyMessage = new MessageBuilder().withRecipientJids(
					fromJid).withBody(msgBody).build();
			// STEP 4
			boolean messageSent = false;
			SendResponse status = xmpp.sendMessage(replyMessage);
			messageSent = (status.getStatusMap().get(fromJid) == SendResponse.Status.SUCCESS);
			// STEP 5
			if (messageSent) {
				strStatus = "Message has been sent successfully";
			} else {
				strStatus = "Message could not be sent";
			}
			_log.info(strStatus);
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getMessage());
		}
	}
}