package org.ei.drishti.web;

import static ch.lambdaj.collection.LambdaCollections.with;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import java.net.HttpURLConnection;
import javax.servlet.http.HttpServletRequest;

import org.ei.drishti.dto.form.FormSubmissionDTO;
import org.ei.drishti.form.domain.FormSubmission;
import org.ei.drishti.form.service.FormSubmissionConverter;
import org.ei.drishti.form.service.FormSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.lambdaj.collection.LambdaList;
import ch.lambdaj.function.convert.Converter;

@Controller
@RequestMapping("ping-openmrs")
public class TestTimerClass {

    private FormSubmissionService formSubmissionService;

	@Autowired
	public TestTimerClass(FormSubmissionService formSubmissionService) {
		this.formSubmissionService = formSubmissionService;
	}
	 
	@RequestMapping(method = GET)
	@ResponseBody
	public List<FormSubmissionDTO> testTimer(HttpServletRequest req) throws IOException {
		List<FormSubmission> newSubmissionsForANM = formSubmissionService 
                .getNewSubmissionsForANM("demo1", 1212121211111L, null);
        LambdaList<FormSubmissionDTO> test = with(newSubmissionsForANM).convert(new Converter<FormSubmission, FormSubmissionDTO>() {
            @Override
            public FormSubmissionDTO convert(FormSubmission submission) {
                return FormSubmissionConverter.from(submission);
            }
        });
       
        System.out.println(test);
		try
		{
            String charset = "UTF-8";;

			URL url = new URL("http://125.209.94.150:6671/openmrs/moduleServlet/codbr/codbrWeb?username=admin&password=Admin123");
			java.net.HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			/*conn.getOutputStream().write("I sent a request".getBytes());
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			*/
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
	        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setRequestProperty("Accept-Charset", charset);

            //con.setFixedLengthStreamingMode(request.toString().length());
			//con.addRequestProperty("Referer", "http://blog.dahanne.net");
			// Start the query
//			conn.connect();
			//OutputStream output = con.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), charset), true); // true = autoFlush, important!

			// Send normal param.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"jsoncontent\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset ).append(CRLF);
		    writer.append(CRLF);
		    writer.append(test.toString()).append(CRLF).flush();
		    // End of multipart/form-data.
		    writer.append("--" + boundary + "--").append(CRLF);
		    if (writer != null) writer.close();

		    //if(output != null) output.close(); 
		    	
			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));
	 
			StringBuilder sb = new StringBuilder();
			String output = null;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
				sb.append(output);
			}
			
			System.out.println(sb);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		/*catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
        String s = new String();
        
        return test;
	}
	
	
}
