package com.progress.codeshare.esbservice.fileFilter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQLog;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQMessageException;
import com.sonicsw.xq.XQMessageFactory;
import com.sonicsw.xq.XQParameterInfo;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQService;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceException;
import com.sonicsw.xq.service.sj.MessageUtils;

public class FileFilterService implements XQService {

	// This is the XQLog (the container's logging mechanism).
	private XQLog m_xqLog = null;

	// This is the the log prefix that helps identify this service during
	// logging
	private static String m_logPrefix = "";

	// These hold version information.
	private static int s_major = 2008;

	private static int s_minor = 8;

	private static int s_buildNumber = 192;

	private static final String PARAM_NAME_DIRECTORY_NAME = "directoryName";

	private static final String PARAM_NAME_FILE_EXTENSION_POINT = "fileExtensionPoint";

	private static final String PARAM_NAME_FILE_EXTENSION = "fileExtension";

	private static final String PARAM_NAME_DESTINATION_EXTENSION_POINT = "destinationExtensionPoint";

	private static final String PARAM_NAME_MOVE_EXTENSION_POINT = "moveExtensionPoint";

	/**
	 * Constructor for a FileFilterService
	 */
	public FileFilterService() {
	}

	/**
	 * Initialize the XQService by processing its initialization parameters.
	 * 
	 * <p>
	 * This method implements a required XQService method.
	 * 
	 * @param initialContext
	 *            The Initial Service Context provides access to:<br>
	 *            <ul>
	 *            <li>The configuration parameters for this instance of the
	 *            PassThroughService.</li>
	 *            <li>The XQLog for this instance of the PassThroughService.</li>
	 *            </ul>
	 * @exception XQServiceException
	 *                Used in the event of some error.
	 */
	public void init(XQInitContext initialContext) throws XQServiceException {
		XQParameters params = initialContext.getParameters();
		m_xqLog = initialContext.getLog();
		setLogPrefix(params);

		m_xqLog.logInformation(m_logPrefix + " Initializing ...");

		writeStartupMessage(params);
		writeParameters(params);
		// perform initilization work.

		m_xqLog.logInformation(m_logPrefix + " Initialized ...");
	}

	private static String[] getFileFilter(String directoryName,
			String fileExtensionPoint, String fileExtension,
			boolean moveExtensionPoint, String destinationExtensionPoint) {

		String[] fileFilter;
		int i = 0;

		FilterFiles filter = new FilterFiles();
		StringBuffer xmlHeaderMessage = new StringBuffer();
		StringBuffer xmlTraillerMessage = new StringBuffer();

		File[] files = filter.getFilesAndFilter(directoryName,
				fileExtensionPoint, fileExtension, moveExtensionPoint,
				destinationExtensionPoint);

		if (files != null) {
			fileFilter = new String[files.length];

			xmlHeaderMessage
					.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			xmlHeaderMessage
					.append("<ff:FileFilter xmlns:ff=\"http://www.progress.com/codeshare/esbservice/fileFilter\" version=\"1.1\">");
			xmlTraillerMessage.append("</ff:FileFilter>");

			for (File file : files) {

				StringBuffer xmlMessage = new StringBuffer();

				if (file != null) {
					xmlMessage.append("<ff:File>");
					xmlMessage.append("<ff:FileName>" + file.getName()
							+ "</ff:FileName>");
					xmlMessage.append("<ff:PathFile>"
							+ file.getPath().replaceAll("\\\\", "/")
							+ "</ff:PathFile>");
					xmlMessage.append("</ff:File>");

					fileFilter[i] = xmlHeaderMessage.toString()
							+ xmlMessage.toString()
							+ xmlTraillerMessage.toString();
					i++;
				}
			}
		} else {
			fileFilter = null;
		}
		return fileFilter;
	}

	private String getHeaderValue(String arguments, XQMessage msg)
			throws XQMessageException {

		final String PARAM_REPLACE_START = "{H";
		final String PARAM_REPLACE_END = "}";
		final String PARAM_REPLACE_DEFAULT = "";

		m_xqLog.logDebug(m_logPrefix + "getHeaderValue arguments : ["
				+ arguments + "]");

		while (arguments.indexOf(PARAM_REPLACE_START) >= 0) {

			String propertyName = arguments.substring((arguments
					.indexOf(PARAM_REPLACE_START) + PARAM_REPLACE_START
					.length()), arguments.indexOf(PARAM_REPLACE_END));

			String replace = arguments.substring(0,
					arguments.indexOf(PARAM_REPLACE_END)).replace(
					PARAM_REPLACE_START, PARAM_REPLACE_DEFAULT).replace(
					PARAM_REPLACE_END, PARAM_REPLACE_DEFAULT);

			arguments = replace
					+ arguments
							.substring(arguments.indexOf(PARAM_REPLACE_END) + 1);

			arguments = arguments.replaceAll(propertyName, msg.getHeaderValue(
					propertyName).toString());

		}

		m_xqLog.logDebug(m_logPrefix + "getHeaderValue arguments : ["
				+ arguments + "]");

		return arguments;
	}

	private String getSimpleDateFormat(String arguments) {
		Date dateNow = new Date();

		final String PARAM_REPLACE_START = "{D";
		final String PARAM_REPLACE_END = "}";
		final String PARAM_REPLACE_DEFAULT = "";

		m_xqLog.logDebug(m_logPrefix + "getSimpleDateFormat arguments : ["
				+ arguments + "]");

		while (arguments.indexOf(PARAM_REPLACE_START) >= 0) {

			String dateFormat = arguments.substring((arguments
					.indexOf(PARAM_REPLACE_START) + PARAM_REPLACE_START
					.length()), arguments.lastIndexOf(PARAM_REPLACE_END));

			String replace = arguments.substring(0,
					arguments.indexOf(PARAM_REPLACE_END)).replace(
					PARAM_REPLACE_START, PARAM_REPLACE_DEFAULT).replace(
					PARAM_REPLACE_END, PARAM_REPLACE_DEFAULT);

			SimpleDateFormat formato = new SimpleDateFormat(dateFormat);

			arguments = replace
					+ arguments
							.substring(arguments.indexOf(PARAM_REPLACE_END) + 1);

			arguments = arguments.replaceAll(dateFormat, formato
					.format(dateNow).toString());

		}

		m_xqLog.logDebug(m_logPrefix + "getSimpleDateFormat arguments : ["
				+ arguments + "]");

		return arguments;
	}

	private void fileFilterServiceContext(XQServiceContext ctx)
			throws XQServiceException {

		try {

			final XQParameters params = ctx.getParameters();

			String directoryName = params.getParameter(
					PARAM_NAME_DIRECTORY_NAME, XQConstants.PARAM_STRING);

			final String fileExtensionPoint = params.getParameter(
					PARAM_NAME_FILE_EXTENSION_POINT, XQConstants.PARAM_STRING);

			final String fileExtension = params.getParameter(
					PARAM_NAME_FILE_EXTENSION, XQConstants.PARAM_STRING);

			String destinationExtensionPoint = params.getParameter(
					PARAM_NAME_DESTINATION_EXTENSION_POINT,
					XQConstants.PARAM_STRING);

			final boolean moveExtensionPoint = params.getBooleanParameter(
					PARAM_NAME_MOVE_EXTENSION_POINT, XQConstants.PARAM_STRING);

			// Get the message.
			XQEnvelope env = null;

			final XQMessageFactory msgFactory = ctx.getMessageFactory();

			while (ctx.hasNextIncoming()) {

				env = ctx.getNextIncoming();
				
				XQMessage msg = env.getMessage();
				
				if (env != null) {

					String[] fileFilter;

					directoryName = getSimpleDateFormat(directoryName);
					directoryName = getHeaderValue(directoryName, msg);
					
					destinationExtensionPoint = getSimpleDateFormat(destinationExtensionPoint);
					destinationExtensionPoint = getHeaderValue(destinationExtensionPoint, msg);

					fileFilter = getFileFilter(directoryName,
							fileExtensionPoint, fileExtension,
							moveExtensionPoint, destinationExtensionPoint);

					for (String file : fileFilter) {

						if (file != null) {
							
							final XQMessage newMsg = msgFactory.createMessage();

							/*
							 * Copy all headers from the original message to the
							 * new message
							 */
							MessageUtils.copyAllHeaders(msg, newMsg);

							final XQPart newPart = newMsg.createPart();

							newPart.setContentId("0");

							newPart.setContent(file,
									XQConstants.CONTENT_TYPE_XML);

							newMsg.addPart(newPart);

							env.setMessage(newMsg);

							final Iterator addressIterator = env.getAddresses();

							if (addressIterator.hasNext())
								ctx.addOutgoing(env);

						}
					}

				}
			}
		} catch (final Exception e) {
			throw new XQServiceException(e);
		}
	}

	/**
	 * Handle the arrival of XQMessages in the INBOX.
	 * 
	 * <p>
	 * This method implement a required XQService method.
	 * 
	 * @param ctx
	 *            The service context.
	 * @exception XQServiceException
	 *                Thrown in the event of a processing error.
	 */
	public void service(XQServiceContext ctx) throws XQServiceException {

		if (ctx == null)
			throw new XQServiceException("Service Context cannot be null.");
		else {
			fileFilterServiceContext(ctx);
		}

	}

	/**
	 * Clean up and get ready to destroy the service.
	 * 
	 * <p>
	 * This method implement a required XQService method.
	 */
	public void destroy() {
		m_xqLog.logInformation(m_logPrefix + "Destroying...");

		m_xqLog.logInformation(m_logPrefix + "Destroyed...");
	}

	/**
	 * Called by the container on container start.
	 * 
	 * <p>
	 * This method implement a required XQServiceEx method.
	 */
	public void start() {
		m_xqLog.logInformation(m_logPrefix + "Starting...");

		m_xqLog.logInformation(m_logPrefix + "Started...");
	}

	/**
	 * Called by the container on container stop.
	 * 
	 * <p>
	 * This method implement a required XQServiceEx method.
	 */
	public void stop() {
		m_xqLog.logInformation(m_logPrefix + "Stopping...");

		m_xqLog.logInformation(m_logPrefix + "Stopped...");
	}

	/**
	 * Clean up and get ready to destroy the service.
	 * 
	 */
	protected void setLogPrefix(XQParameters params) {
		String serviceName = params.getParameter(
				XQConstants.PARAM_SERVICE_NAME, XQConstants.PARAM_STRING);
		m_logPrefix = "[ " + serviceName + " ]";
	}

	/**
	 * Provide access to the service implemented version.
	 * 
	 */
	protected String getVersion() {
		return s_major + "." + s_minor + ". build " + s_buildNumber;
	}

	/**
	 * Writes a standard service startup message to the log.
	 */
	protected void writeStartupMessage(XQParameters params) {

		final StringBuffer buffer = new StringBuffer();

		String serviceTypeName = params.getParameter(
				XQConstants.SERVICE_PARAM_SERVICE_TYPE,
				XQConstants.PARAM_STRING);

		buffer.append("\n\n");
		buffer.append("\t\t " + serviceTypeName + "\n ");

		buffer.append("\t\t Version ");
		buffer.append(" " + getVersion());
		buffer.append("\n");

		buffer
				.append("\t\t Copyright (c) 2008, Progress Sonic Software Corporation.");
		buffer.append("\n");

		buffer.append("\t\t All rights reserved. ");
		buffer.append("\n");

		m_xqLog.logInformation(buffer.toString());
	}

	/**
	 * Writes parameters to log.
	 */
	protected void writeParameters(XQParameters params) {

		final Map map = params.getAllInfo();
		final Iterator iter = map.values().iterator();

		while (iter.hasNext()) {
			final XQParameterInfo info = (XQParameterInfo) iter.next();

			if (info.getType() == XQConstants.PARAM_XML) {
				m_xqLog.logDebug(m_logPrefix + "Parameter Name =  "
						+ info.getName());
			} else if (info.getType() == XQConstants.PARAM_STRING) {
				m_xqLog.logDebug(m_logPrefix + "Parameter Name = "
						+ info.getName());
			}

			if (info.getRef() != null) {
				m_xqLog.logDebug(m_logPrefix + "Parameter Reference = "
						+ info.getRef());

				// If this is too verbose
				// /then a simple change from logInformation to logDebug
				// will ensure file content is not displayed
				// unless the logging level is set to debug for the ESB
				// Container.
				m_xqLog.logDebug(m_logPrefix
						+ "----Parameter Value Start--------");
				m_xqLog.logDebug("\n" + info.getValue() + "\n");
				m_xqLog.logDebug(m_logPrefix
						+ "----Parameter Value End--------");
			} else {
				m_xqLog.logDebug(m_logPrefix + "Parameter Value = "
						+ info.getValue());
			}
		}
	}
}