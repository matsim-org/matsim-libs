<?xml version="1.0" encoding="utf-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >
  <xsl:template match="scores">
    <CategoryDataset>
		<Series name = "average Average Score">
			<xsl:for-each select="score">
				<Item>
					<Key><xsl:value-of select="iteration"/></Key>
					<Value><xsl:value-of select="averageAverage"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
		<Series name = "average Best Score">
			<xsl:for-each select="score">
				<Item>
					<Key><xsl:value-of select="iteration"/></Key>
					<Value><xsl:value-of select="averageBest"/></Value>				
				</Item>			
			</xsl:for-each>	
		</Series>
		<Series name = "average Worst Score">
			<xsl:for-each select="score">
				<Item>
					<Key><xsl:value-of select="iteration"/></Key>
					<Value><xsl:value-of select="averageWorst"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
		<Series name = "average Executed Score">
			<xsl:for-each select="score">
				<Item>
					<Key><xsl:value-of select="iteration"/></Key>
					<Value><xsl:value-of select="averageExecuted"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
    </CategoryDataset>
  </xsl:template>
</xsl:transform>
