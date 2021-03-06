<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
  under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.  For additional information regarding
  copyright in this work, please see the NOTICE file in the top level
  directory of this distribution.

  Source file modified from the original ASF source; all changes made
  are also under Apache License.
--%>
<%@ include file="/WEB-INF/jsps/tightblog-taglibs.jsp" %>

<script>
    var contextPath = "${pageContext.request.contextPath}";
    // Below populated for logged-in user profile edit only
    var userId = "<c:out value='${authenticatedUser.id}'/>";
</script>

<div id="template">

<div id="successMessageDiv" class="alert alert-success" role="alert" v-show="showSuccessMessage" v-cloak>
    <c:choose>
        <c:when test="${authenticatedUser != null}">
            <p><fmt:message key="generic.changes.saved"/></p>
        </c:when>
        <c:otherwise>
            <div v-show="userBeingEdited.status == 'ENABLED'">
                <p><fmt:message key="welcome.accountCreated"/></p>
                <p><a href="<c:url value='/tb-ui/app/login-redirect'/>">
                    <fmt:message key="welcome.clickHere"/></a>
                    <fmt:message key="welcome.toLoginAndPost"/></p>
            </div>
            <div v-show="userBeingEdited.status == 'REGISTERED'">
                <p><fmt:message key="welcome.accountCreated"/></p>
                <p><fmt:message key="welcome.user.account.not.activated"/></p>
            </div>
        </c:otherwise>
    </c:choose>
    <button type="button" class="close" v-on:click="showSuccessMessage = false" aria-label="Close">
       <span aria-hidden="true">&times;</span>
    </button>
</div>

<error-list-message-box v-bind:in-error-obj="errorObj" @close-box="errorObj.errors=null"></error-list-message-box>

<c:choose>
    <c:when test="${authenticatedUser == null}">
        <c:set var="usernameTipKey">userRegister.tip.userName</c:set>
        <c:set var="passwordTipKey">userRegister.tip.password</c:set>
        <c:set var="passwordConfirmTipKey">userRegister.tip.passwordConfirm</c:set>
        <c:set var="saveButtonText">userRegister.button.save</c:set>
        <input type="hidden" id="cancelURL" value="${pageContext.request.contextPath}"/>
        <c:url var="refreshUrl" value="/tb-ui/app/register"/>
        <div ng="profileUserId">
            <p><fmt:message key="userRegister.prompt"/></p>
        </div>
    </c:when>
    <c:otherwise>
        <c:set var="usernameTipKey">userSettings.tip.username</c:set>
        <c:set var="passwordTipKey">userSettings.tip.password</c:set>
        <c:set var="passwordConfirmTipKey">userSettings.tip.passwordConfirm</c:set>
        <c:set var="saveButtonText">generic.save</c:set>
        <input type="hidden" id="cancelURL" value="<c:url value='/tb-ui/app/home'/>"/>
        <p class="subtitle"><fmt:message key="profile.subtitle"/></p>
        <c:url var="refreshUrl" value="/tb-ui/app/profile"/>
    </c:otherwise>
</c:choose>

<input id="refreshURL" type="hidden" value="${refreshURL}"/>

<table class="formtable">
  <tr>
      <td class="label"><label for="userName"><fmt:message key="userSettings.username"/></label></td>
      <td class="field">
        <c:choose>
            <c:when test="${authenticatedUser == null}">
                <input id="userName" type="text" size="30" v-model="userBeingEdited.userName" minlength="5" maxlength="25">
            </c:when>
            <c:otherwise>
                <input id="userName" type="text" size="30" v-model="userBeingEdited.userName" readonly>
            </c:otherwise>
        </c:choose>
      </td>
      <td class="description"><fmt:message key="${usernameTipKey}"/></td>
  </tr>

  <tr>
      <td class="label"><label for="screenName"><fmt:message key="userSettings.screenname"/></label></td>
      <td class="field"><input id="screenName" type="text" size="30" v-model="userBeingEdited.screenName" minlength="3" maxlength="30"></td>
      <td class="description"><fmt:message key="userRegister.tip.screenName"/></td>
  </tr>

  <tr>
      <td class="label"><label for="emailAddress"><fmt:message key="userSettings.email" /></label></td>
      <td class="field"><input id="emailAddress" type="email" size="40" v-model="userBeingEdited.emailAddress" maxlength="40"></td>
      <td class="description"><fmt:message key="userAdmin.tip.email" /></td>
  </tr>

    <tr>
       <td class="label"><label for="passwordText"><fmt:message key="userSettings.password"/></label></td>
        <td class="field">
            <input id="passwordText" type="password" size="20" v-model="userCredentials.passwordText" minlength="8" maxlength="20" autocomplete="new-password">
        </td>
        <td class="description"><fmt:message key="${passwordTipKey}"/></td>
    </tr>
    <tr>
        <td class="label"><label for="passwordConfirm"><fmt:message key="userSettings.passwordConfirm"/></label></td>
        <td class="field">
            <input id="passwordConfirm" type="password" size="20" v-model="userCredentials.passwordConfirm" minlength="8" maxlength="20" autocomplete="new-password">
        </td>
        <td class="description"><fmt:message key="${passwordConfirmTipKey}"/></td>
    </tr>
</table>

<br/>

<div class="control" v-show="!hideButtons">
    <button type="button" class="buttonBox" v-on:click="updateUser()"><fmt:message key='${saveButtonText}'/></button>
    <button type="button" class="buttonBox" v-on:click="cancelChanges()"><fmt:message key='generic.cancel'/></button>
</div>

</div>

<script src="<c:url value='/tb-ui/scripts/components/messages.js'/>"></script>
<script src="<c:url value='/tb-ui/scripts/profile.js'/>"></script>

