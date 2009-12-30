<?xml version="1.0" encoding="utf-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" 
xmlns:sc="http://www.matsim.org/files/dtd">
  <xsl:template match="sc:scores">
    <CategoryDataset>
		<Series name = "average Average Score">
			<xsl:for-each select="sc:score">
				<Item>
					<Key><xsl:value-of select="sc:iteration"/></Key>
					<Value><xsl:value-of select="sc:averageAverage"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
		<Series name = "average Best Score">
			<xsl:for-each select="sc:score">
				<Item>
					<Key><xsl:value-of select="sc:iteration"/></Key>
					<Value><xsl:value-of select="sc:averageBest"/></Value>				
				</Item>			
			</xsl:for-each>	
		</Series>
		<Series name = "average Worst Score">
			<xsl:for-each select="sc:score">
				<Item>
					<Key><xsl:value-of select="sc:iteration"/></Key>
					<Value><xsl:value-of select="sc:averageWorst"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
		<Series name = "average Executed Score">
			<xsl:for-each select="sc:score">
				<Item>
					<Key><xsl:value-of select="sc:iteration"/></Key>
					<Value><xsl:value-of select="sc:averageExecuted"/></Value>				
				</Item>			
			</xsl:for-each>		
		</Series>
    </CategoryDataset>
  </xsl:template>
</xsl:transform>
