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

<c:url var="mediaFileViewUrl" value="/tb-ui/app/authoring/mediaFileView">
    <c:param name="weblogId" value="${actionWeblog.id}" />
</c:url>

<script>
    var contextPath = "${pageContext.request.contextPath}";
    var weblogId = "<c:out value='${actionWeblog.id}'/>";
    var mediaFileId = "<c:out value='${param.mediaFileId}'/>";
    var directoryId = "<c:out value='${param.directoryId}'/>";
    var mediaViewUrl = "<c:out value='${mediaFileViewUrl}'/>";
</script>

<div id="template">

<error-list-message-box v-bind:in-error-obj="errorObj" @close-box="errorObj.errors=null"></error-list-message-box>
 
<c:choose>
    <c:when test="${param.mediaFileId != null}">
        <c:set var="subtitleKey">mediaFileEdit.subtitle</c:set>
        <c:set var="mainAction">mediaFileEdit</c:set>
        <c:set var="pageTip">mediaFileEdit.pagetip</c:set>
        <c:url var="refreshUrl" value="/tb-ui/app/authoring/mediaFileEdit">
            <c:param name="weblogId" value="${param.weblogId}"/>
            <c:param name="directoryId" value="${param.directoryId}"/>
            <c:param name="mediaFileId" value="${param.mediaFileId}"/>
        </c:url>
        <div v-if="mediaFileData.imageFile" class="mediaFileThumbnail">
            <a v-bind:href='mediaFileData.permalink' target="_blank">
                <img align="right" alt="thumbnail" v-bind:src='mediaFileData.thumbnailURL'
                     title='<fmt:message key="mediaFileEdit.clickToView" />' />
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <c:set var="subtitleKey">mediaFileAdd.title</c:set>
        <c:set var="mainAction">mediaFileAdd</c:set>
        <c:set var="pageTip">mediaFileAdd.pageTip</c:set>
        <c:url var="refreshUrl" value="/tb-ui/app/authoring/mediaFileAdd">
            <c:param name="weblogId" value="${param.weblogId}"/>
            <c:param name="directoryId" value="${param.directoryId}"/>
        </c:url>
    </c:otherwise>
</c:choose>

    <input id="refreshURL" type="hidden" value="${refreshURL}"/>

    <p class="pagetip">
        <fmt:message key="${pageTip}"/>
    </p>

    <%-- ================================================================== --%>
    <%-- Title, category, dates and other metadata --%>

    <table class="entryEditTable" cellpadding="0" cellspacing="0" width="100%">

        <tr>
            <td class="entryEditFormLabel">
                <label for="fileControl"><fmt:message key="mediaFileEdit.fileLocation" /></label>
            </td>
            <td>
                <input id="fileControl" type="file" ref="myMediaFile" size="30" v-on:change="handleFileUpload()" v-bind:required="myMediaFile.id == null"/>
            </td>
        </tr>

        <tr>
            <td class="entryEditFormLabel">
                <label for="name"><fmt:message key="generic.name" /></label>
            </td>
            <td>
                <input id="name" type="text" v-model="mediaFileData.name" size="40" maxlength="255" required/>
            </td>
        </tr>

        <tr>
            <td class="entryEditFormLabel">
                <label for="altText"><fmt:message key="mediaFileEdit.altText"/><tags:help key="mediaFileEdit.altText.tooltip"/></label>
            </td>
            <td>
                <input id="altText" type="text" v-model="mediaFileData.altText" size="80" maxlength="255"/>
            </td>
        </tr>

        <tr>
            <td class="entryEditFormLabel">
                <label for="titleText"><fmt:message key="mediaFileEdit.titleText"/><tags:help key="mediaFileEdit.titleText.tooltip"/></label>
            </td>
            <td>
                <input id="titleText" type="text" v-model="mediaFileData.titleText" size="80" maxlength="255"/>
            </td>
        </tr>

        <tr>
            <td class="entryEditFormLabel">
                <label for="anchor"><fmt:message key="mediaFileEdit.anchor"/><tags:help key="mediaFileEdit.anchor.tooltip"/></label>
            </td>
            <td>
                <input id="anchor" type="text" v-model="mediaFileData.anchor" size="80" maxlength="255"/>
            </td>
        </tr>

       <tr>
            <td class="entryEditFormLabel">
                <label for="notes"><fmt:message key="generic.notes"/></label>
            </td>
            <td>
                <input id="notes" type="text" v-model="mediaFileData.notes" size="80" maxlength="255"/>
            </td>
       </tr>
    <c:if test="${param.mediaFileId != null}">
       <tr>
            <td class="entryEditFormLabel">
                <fmt:message key="mediaFileEdit.fileInfo" />
            </td>
            <td>
                <b><fmt:message key="mediaFileEdit.fileType"/></b>: {{mediaFileData.contentType}}
                <b><fmt:message key="mediaFileEdit.fileSize"/></b>: {{mediaFileData.length}}
                <b><fmt:message key="mediaFileEdit.fileDimensions"/></b>: {{mediaFileData.width}} x {{mediaFileData.height}} pixels
            </td>
       </tr>

       <tr>
            <td class="entryEditFormLabel">
                <label for="permalink"><fmt:message key="mediaFileEdit.permalink" /></label>
            </td>
            <td>
                <input id="permalink" type="text" size="80" v-bind:value='mediaFileData.permalink' readonly />
            </td>
       </tr>

       <tr>
            <td class="entryEditFormLabel">
                <label for="directoryId"><fmt:message key="mediaFileEdit.folder" /></label>
            </td>
            <td>
                <input id="directoryId" type="text" size="30" v-bind:value='mediaFileData.directory.name' readonly />
            </td>
       </tr>
    </c:if>

    </table>

<br />
<div class="control">
    <button type="button" v-on:click="saveMediaFile()">
        <fmt:message key='generic.save'/>
    </button>
    <a v-bind:href="'<c:out value='${mediaFileViewUrl}'/>&amp;directoryId=' + mediaFileData.directory.id">
        <button type="button">
            <fmt:message key='generic.cancel'/>
        </button>
    </a>
</div>

</div>

<script src="<c:url value='/tb-ui/scripts/components/messages.js'/>"></script>
<script src="<c:url value='/tb-ui/scripts/mediafileedit.js'/>"></script>

