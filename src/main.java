import lejos.hardware.Button;

public class main {

	public static void main(String[] args ) 
	{
		Robot farripas = new Robot();
		//farripas.debugReconhe();
		//farripas.reconheceTabuleiro();
		//farripas.debugMatrizPrint();
		farripas.debugFactores();		
		farripas.calculaBarreiras();
		farripas.imprimeNumBarrreiras();
		System.out.println("Carregue em qualquer botao para comecar o jogo.");
		Button.waitForAnyPress();
		farripas.preenchePeso();
		farripas.meteNoCurral(1);
		
		return;
	}

}




