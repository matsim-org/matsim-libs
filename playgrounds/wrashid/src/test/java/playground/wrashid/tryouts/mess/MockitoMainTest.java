package playground.wrashid.tryouts.mess;

import java.util.LinkedList;
import java.util.List;

import org.mockito.InOrder;

import static org.mockito.Mockito.*;
import junit.framework.TestCase;
public class MockitoMainTest extends TestCase {

	public void testBasic() {
		
		
		List firstMock = mock(List.class);
		 List secondMock = mock(List.class);
		 
		 //using mocks
		 firstMock.add("was called first");
		 secondMock.add("was called second");
		 
		 //create inOrder object passing any mocks that need to be verified in order
		 InOrder inOrder = inOrder(firstMock, secondMock);
		 
		 //following will make sure that firstMock was called before secondMock
		 inOrder.verify(firstMock).add("was called first");
		 inOrder.verify(secondMock).add("was called second");

	}
	
}
