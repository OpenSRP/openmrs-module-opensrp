<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<link href="/openmrs/moduleResources/teammodule/teamModule.css?v=1.1"
	type="text/css" rel="stylesheet">


<h1>Teams</h1>
<c:if test="${not empty searchedTeam}">
	<h3>Search Results for "${searchedTeam}"</h3>
</c:if>
<table>
	<tr>
		<td>Enter Team Name or ID</td>
		<form:form method="post" commandName="searchTeam">
			<td><form:input id="teamName" path="teamName"/></td>
			<td><button type="submit">Search</button></td>
	</form:form>
	</tr>
	
</table>
<c:if test="${empty searchedTeam}">
	<table class="extra">
		<tr>

			<!-- <input type="hidden" value="add" name="type" /> -->

			<td><a href="/openmrs/module/teammodule/addTeam.form">Add
					Team</a></td>

			<td><a
				href="/openmrs/module/teammodule/allMember.form?searchMember=&from=&to=">View
					All Members</a></td>
		</tr>
		<!-- <tr>
		<form name='edit' action="teamList.form">
			<button type="submit">Edit Team</button>
		</form>
	</tr> -->

	</table>
</c:if>
<c:choose>
	<c:when test="${not empty team}">
		<table class="general">
			<tr>
				<th>Edit</th>
				<th>Id</th>
				<th>Team Name</th>
				<th>Date Created</th>
				<th>Location</th>
				<th>No. of members</th>
				<th>Add Member</th>
				<th>Team Lead</th>
				<th>Change TeamLead</th>
				<th>History</th>
			</tr>
			<!-- <c:set var="length" value="${length}"/> -->

			<c:forEach var="team" items="${team}" varStatus="loop">
				<c:if test="${team.voided}">
					<tr>
						<td><a
							href="/openmrs/module/teammodule/editTeam.form?teamId=${team.teamId}">Edit</a>
						</td>
						<td><c:out value="${team.teamIdentifier}" /></td>
						<td valign="top"><c:out value="${team.teamName}" /></td>
						<td><c:out value="${parsedDate[loop.index]}" /></td>
						<td><c:out value="${team.location.name}" /></td>
						<td><a
							href="/openmrs/module/teammodule/teamMember/list.form?teamId=${team.teamId}&member=">
								<c:out value="${length[loop.index]}" />
						</a></td>
						<td>Team is Voided</td>
						<td><c:out value="${teamLead[loop.index]}" /></td>
						<td>Team is Voided</td>
						<td><a
							href="/openmrs/module/teammodule/teamHistory.form?teamId=${team.teamId}">History</a></td>

					</tr>
				</c:if>
				<!-- <form:form action="teamForm.form">
			<input type="hidden" value="edit" name="type" /> -->
				<c:if test="${team.voided == false}">
					<tr>
						<td><a
							href="/openmrs/module/teammodule/editTeam.form?teamId=${team.teamId}">Edit</a>
						</td>
						<td><c:out value="${team.teamIdentifier}" /></td>
						<td><c:out value="${team.teamName}" /></td>
						<!-- 	<td><fmt:formatDate value="${team.dateCreated}"
								pattern="dd-MM-yyyy" /></td> -->
						<td><c:out value="${parsedDate[loop.index]}" /></td>
						<td><c:out value="${team.location.name}" /></td>
						<td><a
							href="/openmrs/module/teammodule/teamMember/list.form?teamId=${team.teamId}">
								<!-- <c:set var="i" value="${status.index}"/> --> <c:out
									value="${length[loop.index]}" />
						</a></td>
						<td><a
							href="/openmrs/module/teammodule/teamMemberAddForm.form?teamId=${team.teamId}">Add
								Member</a></td>
						<td><c:out value="${teamLead[loop.index]}" /></td>
						<td><a
							href="/openmrs/module/teammodule/teamMember/list.form?teamId=${team.teamId}&member=&changeLead=true">
								change TeamLead </a></td>
						<td><a
							href="/openmrs/module/teammodule/teamHistory.form?teamId=${team.teamId}">History</a></td>


						<!-- &teamId=${team.teamId} -->
					</tr>

				</c:if>
				<!-- </form:form> -->
			</c:forEach>

		</table>
	</c:when>
	<c:otherwise>
		<p>No record(s) found</p>
	</c:otherwise>
</c:choose>
<c:if test="${not empty searchedTeam}">
	<p>
		<a href="/openmrs/module/teammodule/team.form">Back to Team List</a>
	</p>
</c:if>



<%@ include file="/WEB-INF/template/footer.jsp"%>