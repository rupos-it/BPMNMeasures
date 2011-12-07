<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.wfmc.org/2009/XPDL2.2" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:itp="http://www.itp-commerce.com/BPMN2.0" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<!--to set coordinate scale change value of $scale (pixels per inch)-->
	<xsl:variable name="scale" select="'72'"/>
	<xsl:variable name="allShapes" select="//bpmndi:BPMNShape"/>
	<xsl:variable name="allEdges" select="//bpmndi:BPMNEdge"/>
	<xsl:variable name="allElements" select="//bpmn:*[@id]"/>
	<xsl:variable name="processes" select="//bpmn:process"/>
	<xsl:template match="bpmn:definitions">
		<xsl:param name="subproc"/>
		<xsl:param name="proc"/>
		<xsl:param name="testpage"/>
		<xsl:param name="testshape"/>
		<xsl:param name="testelem"/>
		<xsl:param name="testelemtype"/>
		<xsl:param name="testlane"/>
		<xsl:variable name="bpmn" select="."/>
		<xsl:variable name="subprocesses" select="//bpmn:subProcess"/>
		<xsl:variable name="participants" select="//bpmn:participant"/>
		<xsl:variable name="childPages" as="item()*">
			<xsl:for-each select="//bpmndi:BPMNPlane[@bpmnElement]">
				<xsl:for-each select="$subprocesses[@id=string(current()/@bpmnElement)]">
					<xsl:sequence select="current()/.."/>
				</xsl:for-each>
			</xsl:for-each>
		</xsl:variable>
		<Package>
			<xsl:attribute name="xsi:schemaLocation" select="'http://www.wfmc.org/2009/XPDL2.2  http://www.xpdl.org/nugen/p/gseonklyf/a/bpmnxpdl_40a.xsd'"/>
			<xsl:attribute name="Id"><xsl:value-of select="concat('pkg.',generate-id())"/></xsl:attribute>
			<xsl:attribute name="Name"><xsl:value-of select="@itp:name"/></xsl:attribute>
			<PackageHeader>
				<XPDLVersion>2.2</XPDLVersion>
				<Vendor>bpm-tools.com</Vendor>
				<Created>
					<xsl:value-of select="current-dateTime()"/>
				</Created>
				<ModificationDate>
					<xsl:value-of select="current-dateTime()"/>
				</ModificationDate>
			</PackageHeader>
			<xsl:variable name="page" select="bpmndi:BPMNDiagram"/>
			<Pages>
				<xsl:for-each select="$page">
					<Page>
						<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
						<xsl:attribute name="Id"><xsl:value-of select="if(bpmndi:BPMNPlane/@id) then bpmndi:BPMNPlane/@id else @id"/></xsl:attribute>
					</Page>
				</xsl:for-each>
			</Pages>
			<Pools>
				<xsl:for-each select="$page">
					<xsl:variable name="thePage" select="."/>
					<xsl:variable name="pageid" select="if($thePage/bpmndi:BPMNPlane/@id) then $thePage/bpmndi:BPMNPlane/@id else $thePage/@id"/>
					<xsl:variable name="shapes" select="*/bpmndi:BPMNShape"/>
					<!--$shapes is list of shapes on the page-->
					<xsl:variable name="elementRefs" select="$shapes/@bpmnElement"/>
					<!--$elementRefs is list of ids of semantic elements on the page-->
					<xsl:variable name="processLevel" select="if($childPages[@id=current()/@id] ) then xs:string($thePage/bpmndi:BPMNPlane/@bpmnElement) else ''"/>
					<!--for child level pages only, $processLevel is id of the calling subprocess-->
					<xsl:for-each select="$participants">
						<xsl:variable name="theParticipant" select="."/>
						<xsl:variable name="theProcess" select="$processes[@id=string($theParticipant/@processRef)]"/>
						<xsl:variable name="theShape" select="if($bpmn/@exporter='Process Modeler 5 for Microsoft Visio') then $shapes[string(@bpmnElement)=$theProcess/@id] else $shapes[string(@bpmnElement)=$theParticipant/@id]"/>
						<xsl:choose>
							<!--<xsl:when test="$theParticipant[@processRef] and $shapes[string(@bpmnElement)=$theParticipant/@id"> should be this-->
							<xsl:when test="$theParticipant[@processRef] and $theShape">
								<!-- visible process pool (not black box)-->
								<Pool>
									<xsl:attribute name="Id"><xsl:value-of select="concat($theParticipant/@processRef,'.',$pageid)"/></xsl:attribute>
									<xsl:attribute name="Name"><xsl:value-of select="$theParticipant/@name"/></xsl:attribute>
									<!--if top level page and exporter is itp, then $theShape/@bpmnElement points to process; otherwise $theShape/@bpmnElement points to participant.  If child level page then $theShape/@bpmnElement points to a subprocess  -->
									<xsl:attribute name="Process"><xsl:choose><xsl:when test="$childPages[@id=$thePage/@id]"><xsl:value-of select="concat($thePage/*/bpmndi:BPMNPlane/@bpmnElement,'.set')"/></xsl:when><xsl:otherwise><xsl:value-of select="$theParticipant/@processRef"/></xsl:otherwise></xsl:choose></xsl:attribute>
									<xsl:attribute name="BoundaryVisible">true</xsl:attribute>
									<xsl:choose>
										<!-- if this page is a child level page-->
										<xsl:when test="$childPages[@id=$thePage/@id]">
											<xsl:for-each select="$allElements[@id=$processLevel]/bpmn:laneSet">
												<Lanes>
													<xsl:for-each select="bpmn:lane">
														<xsl:variable name="theLaneShape" select="if($thePage/bpmndi:BPMNPlane/@id) then $shapes[string(@bpmnElement)=current()/@id and ../@id=$pageid] else $shapes[string(@bpmnElement)=current()/@id and ../../@id=$pageid]"/>
														<xsl:if test="$theLaneShape">
															<Lane>
																<xsl:attribute name="Id" select="concat(@id,'.',$pageid)"/>
																<xsl:attribute name="Name" select="@name"/>
																<xsl:call-template name="getNodeGraphicsInfo">
																	<xsl:with-param name="testpageid" select="$pageid"/>
																	<xsl:with-param name="testshape" select="$theLaneShape"/>
																	<xsl:with-param name="testelem"/>
																	<xsl:with-param name="testlane"/>
																</xsl:call-template>
															</Lane>
														</xsl:if>
													</xsl:for-each>
												</Lanes>
											</xsl:for-each>
										</xsl:when>
										<xsl:otherwise>
											<!--this page is a top level page-->
											<xsl:variable name="theProcess1" select="$processes[@id=string($theParticipant/@processRef)]"/>
											<xsl:for-each select="$theProcess1/bpmn:laneSet">
												<Lanes>
													<xsl:for-each select="bpmn:lane">
														<xsl:variable name="theLaneShape" select="if($thePage/bpmndi:BPMNPlane/@id) then $shapes[string(@bpmnElement)=current()/@id and ../@id=$pageid] else $shapes[string(@bpmnElement)=current()/@id and ../../@id=$pageid]"/>
														<xsl:if test="$theLaneShape">
															<Lane>
																<xsl:attribute name="Id" select="concat(@id,'.',$pageid)"/>
																<xsl:attribute name="Name" select="@name"/>
																<xsl:call-template name="getNodeGraphicsInfo">
																	<xsl:with-param name="testpageid" select="$pageid"/>
																	<xsl:with-param name="testshape" select="$theLaneShape"/>
																	<xsl:with-param name="testelem"/>
																	<xsl:with-param name="testlane"/>
																</xsl:call-template>
															</Lane>
														</xsl:if>
													</xsl:for-each>
												</Lanes>
											</xsl:for-each>
										</xsl:otherwise>
									</xsl:choose>
									<xsl:call-template name="getNodeGraphicsInfo">
										<xsl:with-param name="testpageid" select="$pageid"/>
										<xsl:with-param name="testshape" select="$theShape"/>
										<xsl:with-param name="testelem"/>
										<xsl:with-param name="testlane"/>
									</xsl:call-template>
								</Pool>
							</xsl:when>
							<xsl:when test="$theParticipant[not(@processRef)] and $shapes[string(@bpmnElement)=$theParticipant/@id]">
								<!--black box pool-->
								<Pool>
									<xsl:attribute name="Id"><xsl:value-of select="concat($theParticipant/@id,'.',$pageid)"/></xsl:attribute>
									<xsl:attribute name="Name"><xsl:value-of select="$theParticipant/@name"/></xsl:attribute>
									<xsl:attribute name="BoundaryVisible">true</xsl:attribute>
									<xsl:call-template name="getNodeGraphicsInfo">
										<xsl:with-param name="testpageid" select="$pageid"/>
										<xsl:with-param name="testshape" select="$shapes[string(@bpmnElement)=$theParticipant/@id]"/>
										<xsl:with-param name="testelem"/>
										<xsl:with-param name="testlane"/>
									</xsl:call-template>
								</Pool>
							</xsl:when>
						</xsl:choose>
					</xsl:for-each>
					<!--check for invisible pool on the page-->
					<xsl:for-each select="$processes">
						<xsl:variable name="theProcess" select="."/>
						<xsl:variable name="processParticipant" select="$participants[string(@processRef)=current()/@id]"/>
						<!--$processParticipant is null if no pools exist in .bpmn-->
						<!--$shapes is set of bpmndi shapes on the page-->
						<!--$procElemsOnPage is a sequence of elements on the page-->
						<xsl:variable name="procElemsOnPage" as="item()*">
							<xsl:for-each select="$shapes">
								<xsl:variable name="elemID" select="string(@bpmnElement)"/>
								<xsl:for-each select="$theProcess//*[@id=$elemID]">
									<xsl:sequence select="."/>
								</xsl:for-each>
							</xsl:for-each>
						</xsl:variable>
						<!--test below checks if any items in $procElemsOnPage and process does not have a visible pool on the page-->
						<xsl:if test="count($procElemsOnPage)>0 and not($processParticipant)">
							<Pool>
								<xsl:attribute name="Id" select="concat($theProcess/@id,'.',$pageid)"/>
								<xsl:attribute name="Name" select="$theProcess/@name"/>
								<xsl:attribute name="Process"><xsl:choose><xsl:when test="$childPages[@id=$thePage/@id]"><xsl:value-of select="concat($thePage/*/@bpmnElement,'.set')"/></xsl:when><xsl:otherwise><xsl:value-of select="$theProcess/@id"/></xsl:otherwise></xsl:choose></xsl:attribute>
								<xsl:attribute name="BoundaryVisible">false</xsl:attribute>
								<xsl:for-each select="($theProcess//bpmn:subProcess | $theProcess)[@id=string($thePage/*/@bpmnElement)]/bpmn:laneSet">
									<Lanes>
										<xsl:for-each select="bpmn:lane">
											<xsl:variable name="theLaneShape" select="if($thePage/bpmndi:BPMNPlane/@id) then $shapes[string(@bpmnElement)=current()/@id and ../@id=$pageid] else $shapes[string(@bpmnElement)=current()/@id and ../../@id=$pageid]"/>
											<xsl:if test="$theLaneShape">
												<Lane>
													<xsl:attribute name="Id" select="concat(@id,'.',$pageid)"/>
													<xsl:attribute name="Name" select="@name"/>
													<xsl:call-template name="getNodeGraphicsInfo">
														<xsl:with-param name="testpageid" select="$pageid"/>
														<xsl:with-param name="testshape" select="$theLaneShape"/>
														<xsl:with-param name="testelem"/>
														<xsl:with-param name="testlane"/>
													</xsl:call-template>
												</Lane>
											</xsl:if>
										</xsl:for-each>
									</Lanes>
								</xsl:for-each>
							</Pool>
						</xsl:if>
					</xsl:for-each>
				</xsl:for-each>
			</Pools>
			<!-- Process Message Flows -->
			<xsl:for-each select="//bpmn:collaboration">
				<MessageFlows>
					<xsl:for-each select="bpmn:messageFlow">
						<xsl:variable name="theEdge" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
						<xsl:variable name="thePlane" select="$theEdge/.."/>
						<xsl:variable name="thePageId" select="if($thePlane/@id) then $thePlane/@id else $thePlane/../@id"/>
						<MessageFlow>
							<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
							<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
							<!--need to adjust @sourceRef and @targetRef in case of black box pools.-->
							<xsl:attribute name="Source">
								<xsl:choose>
									<xsl:when test="string(@sourceRef)=$participants/@id">
										<xsl:value-of select="concat(string(@sourceRef),'.',$thePageId)"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@sourceRef"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:attribute name="Target"><xsl:choose><xsl:when test="string(@targetRef)=$participants/@id"><xsl:value-of select="concat(string(@targetRef),'.',$thePageId)"/></xsl:when><xsl:otherwise><xsl:value-of select="@targetRef"/></xsl:otherwise></xsl:choose></xsl:attribute>
							<!-- Only include the messageVisibleKind attribute if it is present in the edge -->
							<xsl:variable name="visibleKindValue" select="$theEdge/@messageVisibleKind"/>
							<xsl:if test="$visibleKindValue">
							    <xsl:attribute name="messageVisibleKind" select="$visibleKindValue"/>
							</xsl:if>
							<xsl:call-template name="getConnectorGraphicsInfo">
								<xsl:with-param name="testpageid" select="$thePageId"/>
								<xsl:with-param name="testshape" select="$theEdge"/>
							</xsl:call-template>
						</MessageFlow>
					</xsl:for-each>
				</MessageFlows>
			</xsl:for-each>
			<!-- Process associations -->
			<Associations>
				<xsl:for-each select="//bpmn:association">
					<xsl:variable name="theAssociation" select="." />
					<xsl:variable name="theEdge" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
					<xsl:variable name="thePlane" select="$theEdge/.."/>
					<xsl:variable name="thePageId" select="if($thePlane/@id) then $thePlane/@id else $thePlane/../@id"/>
					<Association>
						<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
						<xsl:attribute name="Source"><xsl:value-of select="@sourceRef"/></xsl:attribute>
						<xsl:attribute name="Target"><xsl:value-of select="@targetRef"/></xsl:attribute>
						<xsl:if test="@name">
						<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
						</xsl:if>
						<xsl:if test="@associationDirection">
						<xsl:attribute name="AssociationDirection"><xsl:value-of  select="@associationDirection"/></xsl:attribute>
						</xsl:if>
						<xsl:call-template name="getConnectorGraphicsInfo">
							<xsl:with-param name="testpageid" select="$thePageId" />
							<xsl:with-param name="testshape" select="$theEdge" />
						</xsl:call-template>
					</Association>
				</xsl:for-each>
			</Associations>
			<!-- Process text annotations. -->
			<Artifacts>
				<xsl:for-each select="//bpmn:textAnnotation">
					<Artifact>
						<xsl:variable name="theTextAnnotation" select="." />
						<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
						<xsl:attribute name="ArtifactType">Annotation</xsl:attribute>
						<xsl:attribute name="TextAnnotation"><xsl:value-of select="bpmn:text"/></xsl:attribute>
						<!--now prepare to get graphics info-->
						<xsl:variable name="theShape" select="$allShapes[string(@bpmnElement)=$theTextAnnotation/@id]"/>
						<xsl:variable name="theLane" select="$processes//bpmn:lane[bpmn:flowNodeRef=./@id]"/>
						<xsl:call-template name="getNodeGraphicsInfo">
							<xsl:with-param name="testelem" select="$theTextAnnotation"/>
							<xsl:with-param name="testlane" select="$theLane"/>
							<xsl:with-param name="testpageid" select="if($theShape/../@id) then $theShape/../@id else $theShape/../../@id"/>
							<xsl:with-param name="testshape" select="$theShape"/>
						</xsl:call-template>
					</Artifact>
				</xsl:for-each>
			</Artifacts>
			<WorkflowProcesses>
				<xsl:for-each select="bpmn:process">
					<xsl:variable name="theProcess" select="."/>
					<WorkflowProcess>
						<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
						<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
						<ProcessHeader/>
						<xsl:if test="bpmn:subProcess">
							<ActivitySets>
								<xsl:for-each select="//bpmn:subProcess">
									<xsl:call-template name="Sub">
										<xsl:with-param name="subproc" select="."/>
										<xsl:with-param name="proc" select="$theProcess"/>
									</xsl:call-template>
								</xsl:for-each>
							</ActivitySets>
						</xsl:if>
						<Activities>
							<xsl:for-each select="bpmn:startEvent | bpmn:task | bpmn:manualTask | bpmn:scriptTask | bpmn:userTask | bpmn:serviceTask | bpmn:sendTask | bpmn:receiveTask | bpmn:subProcess | bpmn:callActivity | bpmn:businessRuleTask | bpmn:exclusiveGateway | bpmn:inclusiveGateway | bpmn:eventBasedGateway | bpmn:parallelGateway | bpmn:complexGateway | bpmn:endEvent | bpmn:intermediateCatchEvent | bpmn:intermediateThrowEvent | bpmn:boundaryEvent">
								<xsl:call-template name="getActivity">
									<xsl:with-param name="testelem" select="."/>
								</xsl:call-template>
							</xsl:for-each>
						</Activities>
						<Transitions>
							<xsl:for-each select="bpmn:sequenceFlow">
								<xsl:variable name="theEdge" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
								<xsl:variable name="thePlane" select="$theEdge/.."/>
								<xsl:variable name="thePageId" select="if($thePlane/@id) then $thePlane/@id else $thePlane/../@id"/>
								<xsl:variable name="seqflowId" select="@id"/>
								<xsl:variable name="sourceId" select="@sourceRef"/>
								<xsl:variable name="sourceActivity" select="$theProcess//bpmn:*[@id=$sourceId]"/>
								<Transition>
									<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
									<xsl:attribute name="From"><xsl:value-of select="@sourceRef"/></xsl:attribute>
									<xsl:attribute name="To"><xsl:value-of select="@targetRef"/></xsl:attribute>
									<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
									<xsl:if test="bpmn:conditionExpression">
										<xsl:choose>
											<xsl:when test="$sourceActivity/@default = $seqflowId">
												<Condition>
													<xsl:attribute name="Type">OTHERWISE</xsl:attribute>
												</Condition>
											</xsl:when>
											<xsl:otherwise>
												<Condition>
													<xsl:attribute name="Type">CONDITION</xsl:attribute>
												</Condition>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:if>
									<xsl:call-template name="getConnectorGraphicsInfo">
										<xsl:with-param name="testpageid" select="$thePageId"/>
										<xsl:with-param name="testshape" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
									</xsl:call-template>
								</Transition>
							</xsl:for-each>
						</Transitions>
					</WorkflowProcess>
				</xsl:for-each>
			</WorkflowProcesses>
		</Package>
	</xsl:template>
	<xsl:template name="Sub">
		<xsl:param name="subproc"/>
		<xsl:param name="proc"/>
		<ActivitySet>
			<xsl:attribute name="Id"><xsl:value-of select="concat(@id,'.set')"/></xsl:attribute>
			<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
			<xsl:for-each select="$subproc">
				<Activities>
					<xsl:for-each select="bpmn:startEvent | bpmn:task | bpmn:manualTask | bpmn:scriptTask | bpmn:userTask | bpmn:serviceTask | bpmn:sendTask | bpmn:receiveTask | bpmn:subProcess | bpmn:callActivity | bpmn:exclusiveGateway | bpmn:inclusiveGateway | bpmn:eventBasedGateway | bpmn:parallelGateway | bpmn:complexGateway | bpmn:endEvent | bpmn:intermediateCatchEvent | bpmn:intermediateThrowEvent | bpmn:boundaryEvent">
						<xsl:call-template name="getActivity">
							<xsl:with-param name="testelem" select="."/>
						</xsl:call-template>
					</xsl:for-each>
				</Activities>
			</xsl:for-each>
			<Transitions>
				<xsl:for-each select="bpmn:sequenceFlow">
					<xsl:variable name="theEdge" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
					<xsl:variable name="thePlane" select="$theEdge/.."/>
					<xsl:variable name="thePageId" select="if($thePlane/@id) then $thePlane/@id else $thePlane/../@id"/>
					<xsl:variable name="seqflowId" select="@id"/>
					<xsl:variable name="sourceId" select="@sourceRef"/>
					<xsl:variable name="sourceActivity" select="$proc//bpmn:*[@id=$sourceId]"/>
					<Transition>
						<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
						<xsl:attribute name="From"><xsl:value-of select="@sourceRef"/></xsl:attribute>
						<xsl:attribute name="To"><xsl:value-of select="@targetRef"/></xsl:attribute>
						<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
						<xsl:if test="bpmn:conditionExpression">
							<xsl:choose>
								<xsl:when test="$sourceActivity/@default = $seqflowId">
									<Condition>
										<xsl:attribute name="Type">OTHERWISE</xsl:attribute>
									</Condition>
								</xsl:when>
								<xsl:otherwise>
									<Condition>
										<xsl:attribute name="Type">CONDITION</xsl:attribute>
									</Condition>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
						<xsl:call-template name="getConnectorGraphicsInfo">
							<xsl:with-param name="testpageid" select="$thePageId"/>
							<xsl:with-param name="testshape" select="$allEdges[string(@bpmnElement)=current()/@id]"/>
						</xsl:call-template>
					</Transition>
				</xsl:for-each>
			</Transitions>
		</ActivitySet>
	</xsl:template>
	<xsl:template name="getActivity">
		<xsl:param name="testelem"/>
		<xsl:for-each select="$testelem">
			<Activity>
				<xsl:attribute name="Id"><xsl:value-of select="@id"/></xsl:attribute>
				<xsl:attribute name="Name"><xsl:value-of select="@name"/></xsl:attribute>
				<!-- prepare to get graphics info-->
				<xsl:variable name="theShape" select="$allShapes[string(@bpmnElement)=$testelem/@id]"/>
				<xsl:variable name="theLane" select="$processes//bpmn:lane[bpmn:flowNodeRef=$testelem/@id]"/>
				<xsl:choose>
					<xsl:when test="local-name(.)='startEvent'">
						<Event>
							<StartEvent>
								<xsl:choose>
									<xsl:when test="bpmn:messageEventDefinition">
										<xsl:attribute name="Trigger">Message</xsl:attribute>
										<TriggerResultMessage/>
									</xsl:when>
									<xsl:when test="bpmn:timerEventDefinition">
										<xsl:attribute name="Trigger">Timer</xsl:attribute>
										<TriggerTimer>
											<TimeCycle>
												<xsl:value-of select="@name"/>
											</TimeCycle>
										</TriggerTimer>
									</xsl:when>
									<xsl:when test="bpmn:conditionalEventDefinition">
										<xsl:attribute name="Trigger">Conditional</xsl:attribute>
										<TriggerConditional>
											<Expression>
												<xsl:value-of select="//bpmn:condition"/>
											</Expression>
										</TriggerConditional>
									</xsl:when>
									<xsl:otherwise>
										<xsl:attribute name="Trigger">None</xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</StartEvent>
						</Event>
					</xsl:when>
					<xsl:when test="local-name(.)='subProcess' or local-name(.)='callActivity' or local-name(.)='task'  or local-name(.)='manualTask'  or local-name(.)='scriptTask' or local-name(.)='userTask' or local-name(.)='serviceTask' or local-name(.)='sendTask' or local-name(.)='receiveTask'">
						<xsl:choose>
							<xsl:when test="local-name(.)='subProcess'">
								<BlockActivity>
									<xsl:attribute name="ActivitySetId"><xsl:value-of select="concat(@id,'.set')"/></xsl:attribute>
									<xsl:choose>
										<xsl:when test="$theShape/@isExpanded='true'">
											<xsl:attribute name="View">EXPANDED</xsl:attribute>
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="View">COLLAPSED</xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
								</BlockActivity>
							</xsl:when>
							<xsl:when test="local-name(.)='callActivity'">
								<Implementation>
									<SubFlow>
										<xsl:attribute name="Id"><xsl:value-of select="@calledElement"/></xsl:attribute>
										<xsl:choose>
											<xsl:when test="$theShape/@isExpanded='true'">
												<xsl:attribute name="View">EXPANDED</xsl:attribute>
											</xsl:when>
											<xsl:otherwise>
												<xsl:attribute name="View">COLLAPSED</xsl:attribute>
											</xsl:otherwise>
										</xsl:choose>S
									</SubFlow>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='task'">
								<Implementation>
									<No/>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='userTask'">
								<Implementation>
									<Task>
										<TaskUser/>
									</Task>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='manualTask'">
								<Implementation>
									<Task>
										<TaskManual/>
									</Task>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='scriptTask'">
								<Implementation>
									<Task>
										<TaskScript/>
									</Task>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='serviceTask'">
								<Implementation>
									<Task>
										<TaskService/>
									</Task>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='sendTask'">
								<Implementation>
									<Task>
										<TaskSend/>
									</Task>
								</Implementation>
							</xsl:when>
							<xsl:when test="local-name(.)='receiveTask'">
								<Implementation>
									<Task>
										<TaskReceive>
											<xsl:attribute name="Instantiate" select="'false'"/>
										</TaskReceive>
									</Task>
								</Implementation>
							</xsl:when>
						</xsl:choose>
						<xsl:for-each select="bpmn:standardLoopCharacteristics">
							<Loop>
								<xsl:attribute name="LoopType">Standard</xsl:attribute>
								<LoopStandard/>
							</Loop>
						</xsl:for-each>
						<xsl:for-each select="bpmn:multiInstanceLoopCharacteristics">
							<Loop>
								<xsl:attribute name="LoopType">MultiInstance</xsl:attribute>
								<LoopMultiInstance>
									<xsl:if test="@isSequential=true()">
										<xsl:attribute name="MI_Ordering">Sequential</xsl:attribute>
									</xsl:if>
								</LoopMultiInstance>
							</Loop>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="local-name(.)='exclusiveGateway' or local-name(.)='inclusiveGateway' or local-name(.)='eventBasedGateway' or local-name(.)='parallelGateway'">
						<Route>
							<xsl:attribute name="GatewayType"><xsl:choose><xsl:when test="local-name(.)='exclusiveGateway' or local-name(.)='eventBasedGateway'">Exclusive</xsl:when><xsl:when test="local-name(.)='parallelGateway'">Parallel</xsl:when><xsl:when test="local-name(.)='inclusiveGateway'">Inclusive</xsl:when></xsl:choose></xsl:attribute>
							<xsl:if test="local-name(.)='exclusiveGateway' or local-name(.)='eventBasedGateway'">
								<xsl:attribute name="ExclusiveType" select="if (local-name(.)='exclusiveGateway') then 'Data' else 'Event'"/>
							</xsl:if>
						</Route>
					</xsl:when>
					<xsl:when test="local-name(.)='intermediateThrowEvent' or local-name(.)='intermediateCatchEvent' or local-name(.)='boundaryEvent'">
						<Event>
							<IntermediateEvent>
								<xsl:attribute name="Trigger">
									<xsl:choose>
										<xsl:when test="bpmn:messageEventDefinition">Message</xsl:when>
										<xsl:when test="bpmn:timerEventDefinition">Timer</xsl:when>
										<xsl:when test="bpmn:errorEventDefinition">Error</xsl:when>
										<xsl:when test="bpmn:escalationEventDefinition">Escalation</xsl:when>
										<xsl:otherwise>Other</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
								<xsl:choose>
									<xsl:when test="local-name(.)='boundaryEvent'">
										<xsl:attribute name="Target" select="@attachedToRef"/>
										<xsl:if test="@cancelActivity">
										<xsl:attribute name="Interrupting" select="@cancelActivity"/></xsl:if>
									</xsl:when>
									<xsl:otherwise>
									<xsl:if test="@isInterrupting">
										<xsl:attribute name="Interrupting" select="@isInterrupting"/></xsl:if>
									</xsl:otherwise>
								</xsl:choose>
								
								<xsl:if test="bpmn:messageEventDefinition">
									<TriggerResultMessage>
										<xsl:attribute name="CatchThrow" select="if (local-name(.)='intermediateThrowEvent') then 'THROW' else 'CATCH'  "/>
									</TriggerResultMessage>
								</xsl:if>
								<xsl:if test="bpmn:timerEventDefinition">
									<TriggerTimer>
										<TimeCycle>
											<xsl:value-of select="@name"/>
										</TimeCycle>
									</TriggerTimer>
								</xsl:if>
								<xsl:if test="bpmn:errorEventDefinition">
									<ResultError>
										<xsl:attribute name="ErrorCode" select="@name"/>
									</ResultError>
								</xsl:if>
								<xsl:if test="bpmn:escalationEventDefinition">
									<TriggerEscalation>
										<xsl:attribute name="CatchThrow" select="if (local-name(.)='intermediateThrowEvent') then 'THROW' else 'CATCH'  "/>
										<xsl:attribute name="EscalationCode" select="@name"/>
									</TriggerEscalation>
								</xsl:if>
							</IntermediateEvent>
						</Event>
					</xsl:when>
					<xsl:when test="local-name(.)='endEvent'">
						<Event>
							<EndEvent>
								<xsl:attribute name="Result"><xsl:choose><xsl:when test="bpmn:messageEventDefinition">Message</xsl:when><xsl:when test="bpmn:errorEventDefinition">Error</xsl:when><xsl:when test="bpmn:terminateEventDefinition">Terminate</xsl:when><xsl:otherwise>None</xsl:otherwise></xsl:choose></xsl:attribute>
								<xsl:if test="bpmn:messageEventDefinition">
									<TriggerResultMessage/>
								</xsl:if>
								<xsl:if test="bpmn:errorEventDefinition">
									<ResultError>
										<xsl:attribute name="ErrorCode" select="@name"/>
									</ResultError>
								</xsl:if>
							</EndEvent>
						</Event>
					</xsl:when>
				</xsl:choose>
				<!-- get the graphics info -->
				<xsl:call-template name="getNodeGraphicsInfo">
					<xsl:with-param name="testelem" select="$testelem"/>
					<xsl:with-param name="testlane" select="$theLane"/>
					<xsl:with-param name="testpageid" select="if($theShape/../@id) then $theShape/../@id else $theShape/../../@id"/>
					<xsl:with-param name="testshape" select="$theShape"/>
				</xsl:call-template>
			</Activity>
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="getNodeGraphicsInfo">
		<xsl:param name="testpageid"/>
		<xsl:param name="testshape"/>
		<xsl:param name="testelem"/>
		<xsl:param name="testlane"/>
		<NodeGraphicsInfos>
			<NodeGraphicsInfo>
				<xsl:attribute name="PageId" select="$testpageid"/>
				<xsl:if test="$testlane">
					<xsl:for-each select="$testlane">
						<xsl:attribute name="LaneId" select="concat(@id,'.',$testpageid)"/>
					</xsl:for-each>
				</xsl:if>
				<xsl:attribute name="Height" select="($testshape/dc:Bounds/@height)[1]"/>
				<xsl:attribute name="Width" select="($testshape/dc:Bounds/@width)[1]"/>
				<Coordinates>
					<xsl:attribute name="XCoordinate" select="($testshape/dc:Bounds/@x)[1]"/>
					<xsl:attribute name="YCoordinate" select="($testshape/dc:Bounds/@y)[1]"/>
				</Coordinates>
			</NodeGraphicsInfo>
		</NodeGraphicsInfos>
	</xsl:template>
	<xsl:template name="getConnectorGraphicsInfo">
		<xsl:param name="testpageid"/>
		<xsl:param name="testshape"/>
		<xsl:for-each select="$testshape">
			<ConnectorGraphicsInfos>
				<ConnectorGraphicsInfo>
					<xsl:attribute name="PageId" select="$testpageid"/>
					<xsl:for-each select="di:waypoint">
						<Coordinates>
							<xsl:attribute name="XCoordinate" select="@x"/>
							<xsl:attribute name="YCoordinate" select="@y"/>
						</Coordinates>
					</xsl:for-each>
				</ConnectorGraphicsInfo>
			</ConnectorGraphicsInfos>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
