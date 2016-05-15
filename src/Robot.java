import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorMode;
import lejos.robotics.Color;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class Robot {
	//Atributos
	static RegulatedMotor leftMotor;
	static RegulatedMotor rightMotor;
	final int tempoAndaFrente = 3200;
	static final int tempoRegressoCentro =200;
	private boolean andou = false;
	public static int posicaoRobotX;
	public static int posicaoRobotY;
	public static int [] posicaoOvelha1;
	public static int [] posicaoOvelha2;
	public int  orientacao = Quadrado.CIMA;
	private Tabuleiro tb;
	private EV3GyroSensor gyroscope;
	private EV3TouchSensor sensTouch;
	private EV3UltrasonicSensor ultrasom;
	private EV3ColorSensor senColor;
	private Sound so;
	//Array de vi posição
	boolean[] verificaX;
	boolean[] verificaY;
	boolean jogada1 = true;
	int numPassosOvelha = 0;
	private int novaMigalha;
	private int numBarreira = 0;
	//Contrutor
	public Robot(){
		
		System.out.println("Inicializa motores");
		leftMotor = new EV3LargeRegulatedMotor(BrickFinder.getDefault().getPort("B"));
		rightMotor = new EV3LargeRegulatedMotor(BrickFinder.getDefault().getPort("C"));

		System.out.println("Inicializa sensores");
		Port portaGyro = LocalEV3.get().getPort("S2");
		gyroscope = new EV3GyroSensor(portaGyro); 

		Port portaToque =LocalEV3.get().getPort("S3");
		sensTouch = new EV3TouchSensor(portaToque);


		Port portaUltrasom = LocalEV3.get().getPort("S4");
		ultrasom = new EV3UltrasonicSensor(portaUltrasom);

		Port portaCores = LocalEV3.get().getPort("S1");
		senColor = new EV3ColorSensor(portaCores);
		tb = new Tabuleiro();

		posicaoRobotX = 0;
		posicaoRobotY = 0;
		System.out.println("Limpa matriz de verificacao");
		verificaX = new boolean[6];
		verificaY = new boolean[6];

		for(int i = 0; i < 6; i++)
		{
			verificaX[i] = false;
			verificaY[i] = false;
		}
		novaMigalha = 0;//debugReconhe();

		posicaoOvelha1 = new int[2];
		posicaoOvelha2 = new int[2];
	}

	/**
	 * Metodo que faz com o robot se mova para a frente
	 * @param duration
	 */
	public void frente(int duration){
		leftMotor.setSpeed(200);
		rightMotor.setSpeed(200);
		leftMotor.synchronizeWith(new RegulatedMotor[]{rightMotor});
		leftMotor.startSynchronization();	

		leftMotor.forward();
		rightMotor.forward();

		leftMotor.endSynchronization();
		Delay.msDelay(duration);
		//leftMotor.stop(true);
		//rightMotor.stop(true);

		andou = true;
	}
	/**
	 * Metodo que faz com que o robot se movimente para trás
	 * @param duration
	 */
	public void atras(int duration){
		leftMotor.setSpeed(200);
		rightMotor.setSpeed(200);
		leftMotor.synchronizeWith(new RegulatedMotor[]{rightMotor});
		leftMotor.startSynchronization();	

		leftMotor.backward();
		rightMotor.backward();

		leftMotor.endSynchronization();
		Delay.msDelay(duration);
		//leftMotor.stop(true);
		//rightMotor.stop(true);
	}

	/**
	 *Metodo que faz com o o braço do robot se movimente afim de tocar na ovelha
	 */
	public void toca(){

		SensorMode touch = sensTouch.getTouchMode();
		float[] sample = new float[touch.sampleSize()];
		RegulatedMotor mA = new EV3MediumRegulatedMotor(MotorPort.A);
		mA.resetTachoCount();

		//Motor.A.rotate(-90,true);
		//Motor.A.setSpeed(720);
		mA.setSpeed(720);

		int colorId = senColor.getColorID();
		while(colorId != Color.RED && colorId != Color.BLACK){ //A cor Red na realidade é a cor Laranja
			colorId = senColor.getColorID();
			frente(10);
		}

		getMotorsStop();

		System.out.println("Entra");
		do{
			mA.backward();
			touch.fetchSample(sample, 0);
			System.out.println(sample[0]);
		}while(sample[0] == 0);
		System.out.println("Tocou");
		mA.stop();
		
		mA.rotate(180,true);
		Delay.msDelay(1000);

		atras(tempoRegressoCentro);
		Delay.msDelay(1000);
		getMotorsStop();

		mA.stop();
		mA.close();
		//sensTouch.close();
	}
	/**
	 * Método que faz com que o robot grite para a ovelha para onde está virado
	 */
	public void grita(){
		//File sound = new File(path);

		//Sound.playSample(sound, 100);
		Sound.systemSound(true, 3);
		Delay.msDelay(5000);
	}
	/**
	 * Metodo que faz com que o robot detete barreiras.
	 */
	public void veCor(){

		int colorId = senColor.getColorID();
		boolean timeEnd = false;
		long tempoPretendido = System.currentTimeMillis() + 2100;

		while(colorId != Color.RED && colorId != Color.BLACK){ //A cor Red na realidade é a cor Laranja
			colorId = senColor.getColorID();
			frente(10);
		}
		//Adiciona barreiras ao array na matriz durante o reconhecimento
		if(colorId == Color.RED)
		{
			numBarreira ++;
			if(orientacao == Quadrado.CIMA){
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].alteraBarreira(Quadrado.CIMA);
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].alteraBarreira(Quadrado.BAIXO);
			}
			if(orientacao == Quadrado.ESQUERDA){
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].alteraBarreira(Quadrado.ESQUERDA);
				tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].alteraBarreira(Quadrado.DIREITA);
			}
			if(orientacao == Quadrado.BAIXO){
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].alteraBarreira(Quadrado.BAIXO);
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].alteraBarreira(Quadrado.CIMA);
			}
			if(orientacao == Quadrado.DIREITA){
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].alteraBarreira(Quadrado.DIREITA);
				tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].alteraBarreira(Quadrado.ESQUERDA);
			}
		}
		atras(tempoRegressoCentro);
		andou = false;
		Delay.msDelay(1000);

		so.beep();
	}
	/**
	 * Mede a distancia do robo a um obstaculo
	 * @return
	 */
	public float distancia_ovelha(){
		ultrasom.enable();
		SampleProvider distance = ultrasom.getDistanceMode();

		float[] sample = new float[distance.sampleSize()];

		distance.fetchSample(sample, 0);

		return sample [0];
	}

	/**
	 * Atualiza a posição do robot depois de este rodas e avançar para um novo quadrado
	 */
	public void atualizaPosicao(){
		if (orientacao == Quadrado.CIMA && andou){
			posicaoRobotY++;
		}
		if (orientacao == Quadrado.DIREITA && andou){
			posicaoRobotX++;
		}
		if (orientacao == Quadrado.BAIXO && andou){
			posicaoRobotY--;
		}
		if (orientacao ==  Quadrado.ESQUERDA && andou){
			posicaoRobotX--;
		}
		andou = false;
	}


	/**
	 * Faz o robot rodar para a direita.
	 * @param direcao
	 */
	public void roda()
	{
		//Velocidade motores
		leftMotor.setSpeed(100);
		rightMotor.setSpeed(100);
		//Limpa sensor
		gyroscope.reset();
		//Modo sensor
		SampleProvider degree = gyroscope.getAngleMode();
		float[] valores = new float[degree.sampleSize()];
		degree.fetchSample(valores, 0);


		while(valores[0] < 88 && valores[0] > -88)
		{
			//Reads the value from the angle
			leftMotor.forward();
			rightMotor.backward();					
			degree.fetchSample(valores, 0);
		};
		//System.out.println(valores[0]);
		if(valores[0] < 0)
		{
			if(orientacao == Quadrado.CIMA)
			{
				orientacao = Quadrado.DIREITA;
			}
			else
			{
				orientacao ++;
			}
		}
		else
		{
			if(orientacao == Quadrado.DIREITA)
			{
				orientacao = Quadrado.CIMA;
			}
			else
			{
				orientacao --;
			}
		}

	}
	/**
	 * Metodo que verifica se o robot esta num quadrado em que tem barreiras e se pode avançar 
	 * Usado no reconhecimento
	 * Retorna false se o robo não pode avançar.
	 * @return
	 */
	public boolean verificaLimiteBarreira()
	{
		if(orientacao == Quadrado.CIMA && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
		{
			return false;
		}
		else if(orientacao == Quadrado.BAIXO && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
		{
			return false;
		}
		else if(orientacao == Quadrado.ESQUERDA && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
		{
			return false;
		}
		else if(orientacao == Quadrado.DIREITA && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
		{
			return false;
		}
		else
		{
			veCor();
			return true;
		}

	}
	
	/**
	 * Fecha as portas dos sensores
	 */
	private void fechaSensores() {
		ultrasom.close();
		sensTouch.close();
		gyroscope.close();
		Motor.A.stop();
		Motor.A.close();
	}

	/**
	 * Limita o range que o sensor de ultrasons pode medir
	 * Recebe a dimensao do tabuleiro
	 * Devolve o comprimento máximo para a leitura
	 * @param d
	 * @return
	 */
	public double distanciaMax(double d){
		if(orientacao == Quadrado.DIREITA)
		{
			return (d - (posicaoRobotX + 1) * 0.30);
		}
		else if(orientacao == Quadrado.BAIXO)
		{
			return (d - (5 - (posicaoRobotY + 1)) * 0.30);
		}
		else if(orientacao == Quadrado.ESQUERDA)
		{
			return (d - (5 - (posicaoRobotX + 1)) * 0.30);
		}
		else if(orientacao == Quadrado.CIMA)
		{
			return (d - (posicaoRobotY + 1) * 0.30);
		}
		else
		{
			return d;
		}
	}

	/**
	 * Método para parar os motores.
	 */
	public static void getMotorsStop(){
		leftMotor.synchronizeWith(new RegulatedMotor[]{rightMotor});
		leftMotor.startSynchronization();	

		leftMotor.stop();
		rightMotor.stop();

		leftMotor.endSynchronization();

	}
	
	/**
	 * Verifica os limites de um quadrado em procura de barreiras e ovelhas.
	 */
	void verifica4Lados()
	{
		verificaLimiteBarreira();
		limitaProcura();
		roda();
		verificaLimiteBarreira();
		limitaProcura();
		roda();
		verificaLimiteBarreira();
		limitaProcura();
		roda();
		verificaLimiteBarreira();
		limitaProcura();
	}

	/**
	 * Função que limita a procura da ovelha ao tabuleiro.
	 */
	void limitaProcura()
	{
		if(posicaoRobotX == 0 && posicaoRobotY == 0  && (orientacao == Quadrado.BAIXO || orientacao == Quadrado.ESQUERDA))
		{
			//N procura ovelha
		}
		else if(posicaoRobotX == 5 && posicaoRobotY == 5  && (orientacao == Quadrado.CIMA || orientacao == Quadrado.DIREITA))
		{
			//N procura ovelha
		}
		else if(posicaoRobotX == 0 && posicaoRobotY == 5  && (orientacao == Quadrado.CIMA|| orientacao == Quadrado.ESQUERDA))
		{
			//N procura ovelha
		}
		else if(posicaoRobotX == 5 && posicaoRobotY == 0  && (orientacao == Quadrado.BAIXO|| orientacao == Quadrado.DIREITA))
		{
			//N procura ovelha
		}
		else if(posicaoRobotY == 0  && (orientacao == Quadrado.BAIXO))
		{
			//N procura ovelha
		}
		else if (posicaoRobotY == 5  && (orientacao == Quadrado.CIMA))
		{
			//N procura ovelha
		}
		else if(posicaoRobotX == 0  && (orientacao == Quadrado.ESQUERDA))
		{
			//N procura ovelha
		}
		else if (posicaoRobotX == 5 && (orientacao == Quadrado.DIREITA))
		{
			//N procura ovelha
		}
		else
		{
			if(distancia_ovelha() < 0.30)
			{
				//Marco que a ovelha tá naquela posição
				if(orientacao == Quadrado.CIMA)
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].setOvelha(true);
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].setVisitado(true);
					getMotorsStop();
					System.out.println("Encontrei uma ovelha! Prima em qualquer botao para continuar.");
					Button.waitForAnyPress();
				}
				else if(orientacao == Quadrado.ESQUERDA)
				{
					tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].setOvelha(true);
					tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].setVisitado(true);
					getMotorsStop();
					System.out.println("Encontrei uma ovelha! Prima em qualquer botao para continuar.");
					Button.waitForAnyPress();
				}
				else if(orientacao == Quadrado.BAIXO)
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].setOvelha(true);
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].setVisitado(true);
					getMotorsStop();
					System.out.println("Encontrei uma ovelha! Prima em qualquer botao para continuar.");
					Button.waitForAnyPress();
				}
				else
				{
					tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].setOvelha(true);
					tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].setVisitado(true);
					getMotorsStop();
					System.out.println("Encontrei uma ovelha! Prima em qualquer botao para continuar.");
					Button.waitForAnyPress();
				}
			}
			else
			{
			}
		}
	}

	/**
	 *  Verifica se já visitou todos os quadrados
	 * @return
	 */
	boolean verificaVisita()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				if(!tb.getTabuleiro()[i][j].isVisitado())
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Metodo que faz o robo fazer todo o reconhecimento do tabuleiro.
	 */
	void reconheceTabuleiro()
	{
		novaMigalha++;
		tb.getTabuleiro()[0][0].setMigalha(novaMigalha);
		System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
		while(!verificaVisita())
		{
			System.out.println("Ainda me falta visitar algum.");
			if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].isVisitado())
			{
				System.out.println("Vou verificar os 4 lados");
				verifica4Lados();
			}

			if(posicaoRobotY < 5 && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				frente(tempoAndaFrente);
				atualizaPosicao();
				System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
				{
					novaMigalha++;
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
				}


				System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

			}
			else if(posicaoRobotX < 5 &&!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				frente(2100);
				atualizaPosicao();
				System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
				{
					novaMigalha++;
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
				}


				System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
			}
			else if(posicaoRobotY <= 5 && posicaoRobotY > 0 &&!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
				System.out.println("DEVIA ENTRAR AQUI SEMPRE QUE VOU PARA UM NOVO ABAIXO");
				frente(tempoAndaFrente);
				atualizaPosicao();
				System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
				{
					novaMigalha++;
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
				}

				System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
			}
			else if(posicaoRobotX > 0 &&!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				frente(tempoAndaFrente);
				atualizaPosicao();
				System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
				{
					novaMigalha++;
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
				}
				System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
			}
			else
			{
				if(voltaAtras(true))
				{
					break;
				}
			}
		}
		while(true)
		{
			voltaAtras(false);
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				getMotorsStop();
				break;
			}
		}
		posicaoOvelha();
		if(numBarreira < 6)
		{
			verificaSeFaltaBarr();
		}
		//}
	}
	/**
	 * Metodo que faz o robot voltar atras sempre que fica preso durante o reconhecimento do tabuleiro.
	 * @param bloqueado true sse o robo esta bloqueado e para isso tem de voltar para tras
	 * @return 
	 */
	public boolean voltaAtras(boolean bloqueado)
	{
		boolean checkAdvance = false;
		int[] migalhas;
		int max = 0;
		int posicaoXMigMax = 0;
		int posicaoYMigMax = 0;
		//Robo fica preso
		//Verifica todos os quadrados em volta.

		//todos os quadrados tendo em conta os limites por causa do erro outofbounds
		if(posicaoRobotX == 0 && posicaoRobotY == 0)
		{
			//verificar quadrado acima e a direita
			if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotX == 0 && posicaoRobotY == 5)
		{
			//verificar abaixo direita
			if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if (posicaoRobotX == 5 && posicaoRobotY == 5)
		{
			//vou verificar quadrafdos abaixo e a esquerda
			if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotX == 5 && posicaoRobotY == 0)
		{
			//vou verificar os quadrados acima e a esquerda 
			if( tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotX == 0)
		{
			//verifica quadraos acima abaixo e a direita.
			if( tb.getTabuleiro()[posicaoRobotX +1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotX == 5)
		{
			//verifica quadrados acima abaixo e a esquerda.
			if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY +1 ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotY == 0)
		{
			//Verifica quadrados acima diretira esquerda.
			if( tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else if(posicaoRobotY == 5)
		{

			//Verifica quadrados abaixo esquerda  direita. 
			if( tb.getTabuleiro()[posicaoRobotX ][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;

			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}
		else
		{
			//Verificação dos quatro quadrados caso o robo esteja no meio do tabueleiro
			if((posicaoRobotX > 0 && tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if((posicaoRobotX <5 && tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if((posicaoRobotY > 0 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
			if((posicaoRobotY < 5 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				checkAdvance = true;
			}
			else
			{
				checkAdvance = false;
			}
		}

		if(checkAdvance == true)
		{
			//troca da migalha;
			//	while(true)
			//	{
			tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(0);
			System.out.println("Entrei na coisinha que n devia");
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				migalhas = new int[2];
				int controla = 0;
				//verificar quadrado acima e a direita
				if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}

			}
			else if(posicaoRobotX == 0 && posicaoRobotY == 5)
			{
				migalhas = new int[2];
				int controla = 0;
				//verificar abaixo direita
				if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;
				}
			}
			else if (posicaoRobotX == 5 && posicaoRobotY == 5)
			{
				migalhas = new int[2];
				int controla = 0;
				//vou verificar quadrafdos abaixo e a esquerda
				if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 0)
			{
				migalhas = new int[2];
				int controla = 0;
				//vou verificar os quadrados acima e a esquerda 
				if( tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}
			}
			else if(posicaoRobotX == 0)
			{
				migalhas = new int[3];
				int controla = 0;
				//verifica quadraos acima abaixo e a direita.
				if( tb.getTabuleiro()[posicaoRobotX +1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX +1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;
				}
			}
			else if(posicaoRobotX == 5)
			{
				migalhas = new int[3];
				int controla = 0;
				//verifica quadrados acima abaixo e a esquerda.
				if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY +1 ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}
			}
			else if(posicaoRobotY == 0)
			{
				migalhas = new int[3];
				int controla = 0;
				//Verifica quadrados acima diretira esquerda.
				if( tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
					controla++;
				}
			}
			else if(posicaoRobotY == 5)
			{
				migalhas = new int[3];
				int controla = 0;
				//Verifica quadrados abaixo esquerda  direita. 
				if( tb.getTabuleiro()[posicaoRobotX ][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;

				}
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
					controla++;
				}if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
					controla++;
				}
			}
			else
			{
				migalhas = new int[4];
				int controla = 0;
				//Verificação dos quatro quadrados caso o robo esteja no meio do tabueleiro
				if((posicaoRobotX > 0 && tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if((posicaoRobotX <5 && tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
					controla++;
				}
				if((posicaoRobotY > 0 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
					controla++;
				}
				if((posicaoRobotY < 5 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
					controla++;
				}
			}
			max = migalhas[0];
			for(int i = 0; i < migalhas.length; i++)
			{
				if(migalhas[i]>max)
				{
					max = migalhas[i];
				}
			}
			boolean checkfordentro = false;
			for(int i = 0; i < 6; i++)
			{
				for(int j = 0; j < 6; j++)
				{
					if(tb.getTabuleiro()[i][j].getMigalha() == max)
					{
						checkfordentro =true;
						posicaoXMigMax = i;
						posicaoYMigMax = j;
						break;
					}
				}
				if(checkfordentro == true)
				{
					break;
				}
			}
			System.out.println("Pos Migalha("+posicaoXMigMax+" "+posicaoYMigMax+ ")");
			if(posicaoRobotX > posicaoXMigMax)
			{
				//roda para orientacao esquerda
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
			}
			else if(posicaoRobotX < posicaoXMigMax)
			{
				//roda para orientacao direita
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
			}
			else if(posicaoRobotY > posicaoYMigMax)
			{
				//roda para orientacao baixo
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
			}
			else
			{
				//roda para orientacao cima
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
			}
			tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);

			System.out.print(bloqueado);
			if(bloqueado && verificaVisita())
			{
				return true;
			}
			else if(posicaoRobotX ==0 && posicaoRobotY == 0)
			{
				return true;
			}
			else
			{
				novaMigalha = novaMigalha-1;
				frente(tempoAndaFrente);
				getMotorsStop();
				atualizaPosicao();
				System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Metodo utilizado para que seja evitada a volta de reocnhecimento
	 * @return
	 */
	public int debugReconhe()
	{
		tb.getTabuleiro()[0][2].setOvelha(true);
		tb.getTabuleiro()[0][0].setVisitado(true);
		tb.getTabuleiro()[0][1].setVisitado(true);
		tb.getTabuleiro()[1][1].setVisitado(true);
		tb.getTabuleiro()[1][2].setVisitado(true);
		tb.getTabuleiro()[1][3].setVisitado(true);
		tb.getTabuleiro()[1][4].setVisitado(true);
		
		tb.getTabuleiro()[1][4].alteraBarreira(Quadrado.ESQUERDA);
		tb.getTabuleiro()[0][4].alteraBarreira(Quadrado.DIREITA);

		tb.getTabuleiro()[0][0].setMigalha(1);
		tb.getTabuleiro()[0][1].setMigalha(2);
		tb.getTabuleiro()[1][1].setMigalha(3);
		tb.getTabuleiro()[1][2].setMigalha(4);
		tb.getTabuleiro()[1][3].setMigalha(5);
		tb.getTabuleiro()[1][4].setMigalha(6);


		posicaoRobotX = 1;
		posicaoRobotY = 5;
		orientacao = Quadrado.CIMA;
		int novaMigalha = 7;
		return novaMigalha;
	}
	/**
	 * Metodo imprime a matriz de factores
	 */
	public void debugMatrizPrint()
	{
		for(int i = 0; i <6; i++)
		{
			for(int j = 0; j<6 ; j++)
			{
				System.out.println("X " + j +" Y "+ i + " Ovelha " + tb.getTabuleiro()[j][i].hasOvelha() +
						" Cima " + tb.getTabuleiro()[j][i].getBarreiraL()[Quadrado.CIMA] + 
						" Direita " + tb.getTabuleiro()[j][i].getBarreiraL()[Quadrado.DIREITA] + 
						" Esquerda " + tb.getTabuleiro()[j][i].getBarreiraL()[Quadrado.ESQUERDA] + 
						" Baixo " + tb.getTabuleiro()[j][i].getBarreiraL()[Quadrado.BAIXO]);
			}
		}
	}
/**
 * Metodo que tenta resolver o problema de quando se tem duas barreiras adjacentes, 
 * e que o robot na busca não conseguiu detetar a ultima barreira
 */
	private void verificaSeFaltaBarr()
	{
		//Ovelha a esquerda da outra
		if(posicaoOvelha1[0] == posicaoOvelha2[0] - 1  && posicaoOvelha1[1] == posicaoOvelha2[1])
		{
			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].alteraBarreira(Quadrado.DIREITA);
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].alteraBarreira(Quadrado.ESQUERDA);
		}
		//ovelha a direita da outra
		else if(posicaoOvelha1[0] == posicaoOvelha2[0] + 1 && posicaoOvelha1[1] == posicaoOvelha2[1])
		{

			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].alteraBarreira(Quadrado.ESQUERDA);
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].alteraBarreira(Quadrado.DIREITA);
		}
		//Ovelha em cima da outra
		else if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1] + 1)
		{

			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].alteraBarreira(Quadrado.BAIXO);
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].alteraBarreira(Quadrado.CIMA);
		}
		//ovelha em baixo da outra
		else if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1] - 1)
		{

			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].alteraBarreira(Quadrado.CIMA);
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].alteraBarreira(Quadrado.BAIXO);
		}
		
	}
	
	/*
	 * Coloca em arrays globais as posições das ovelhas encontradas durante o reconhecimento
	 */
	public void posicaoOvelha()
	{
		int k = 0;
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				if(tb.getTabuleiro()[i][j].hasOvelha())
				{
					k++;
					if(k == 1)
					{
						posicaoOvelha1[0] = i;
						posicaoOvelha1[1] = j;
					}
					else
					{

						posicaoOvelha2[0] = i;
						posicaoOvelha2[1] = j;
					}
				}
			}
		}
		if(k != 2)
		{
			posicaoOvelha2[0] = posicaoOvelha1[0];
			posicaoOvelha2[1] = posicaoOvelha1[1];
		}
	}

	/**
	 * Calcula o número de barreiras por cada quadrado
	 */
	public void calculaBarreiras()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				for(int k = 0; k < 4; k++)
				{
					if(tb.getTabuleiro()[i][j].getBarreiraL()[k])
					{
						tb.getTabuleiro()[i][j].setNumBarreiras(tb.getTabuleiro()[i][j].getNumBarreiras() + 1);
					}
				}
			}
		}
	}

	/**
	 * Preenche a matriz com os factores de escolha que o robot vai usar para calcular o percurso
	 * (Percurso até a melhor posição que fica em volta da ovelha)
	 */
	public void preencheFactores(int xQuadrado, int yQuadrado)
	{
		if(xQuadrado < 0 || xQuadrado > 5 || yQuadrado < 0 || yQuadrado > 5)
		{}
		else
		{
			limpaFactores();
			int factor = 0;
			factor = 0;
			//Inicio preenchimento linha
			//Preenche x para direita
			for(int i = xQuadrado + 1; i <6 ; i++)
			{
				factor++;
				tb.getTabuleiro()[i][yQuadrado].setFactorEscolha(factor);
			}
			factor = 0;
			//Preenche x para esquerda
			for(int i = xQuadrado - 1; i >= 0; i--)
			{
				factor++;
				tb.getTabuleiro()[i][yQuadrado].setFactorEscolha(factor);
			}
	
			//Fim preenchimento linha
			//Preenche y
			for(int i = 0; i < 6; i++)
			{
				factor = tb.getTabuleiro()[i][yQuadrado].getFactorEscolha();
				//Preenche y para cima
				for(int j = yQuadrado + 1; j < 6; j++)
				{
					factor++;
					tb.getTabuleiro()[i][j].setFactorEscolha(factor);
				}
				factor = tb.getTabuleiro()[i][yQuadrado].getFactorEscolha();
				//Preenche y para baixo
				for(int j = yQuadrado - 1; j >= 0; j--)
				{
					factor++;
					tb.getTabuleiro()[i][j].setFactorEscolha(factor);
				}
			}
	
			// Percorre toda a matriz e soma ao factor de cada quadrado o numero de barreiras
			for(int i = 0; i < 6; i++)
			{
				for(int j = 0; j < 6; j++)
				{
					tb.getTabuleiro()[i][j].setFactorEscolha(tb.getTabuleiro()[i][j].getFactorEscolha() + tb.getTabuleiro()[i][j].getNumBarreiras());
				}
			}
			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].setFactorEscolha(1000);
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].setFactorEscolha(1000);
			tb.getTabuleiro()[xQuadrado][yQuadrado].setFactorEscolha(0);
		}

	}
	
	/**
	 *  Método que preenche a matriz com pesos a partir do curral.
	 */
	public void preenchePeso()
	{
		int peso = 5;
		for(int i = 0; i < 6; i++)
		{
			tb.getTabuleiro()[i][5].setPeso(peso);
			peso = peso + 1;
		}
		//Exepanir
		for(int i = 0; i < 6; i++)
		{
			peso = tb.getTabuleiro()[i][5].getPeso();
			for(int j = 4; j >= 0; j--)
			{
				peso = peso - 1;
				tb.getTabuleiro()[i][j].setPeso(peso);
			}
		}

	}


	/**
	 * Imprime os factores de cada quadrado para efeitos de debugging
	 */
	public void imprimeFactores()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				System.out.println("X " + i + " Y " + j + " Factor " + tb.getTabuleiro()[i][j].getFactorEscolha());
			}
		}
	}
	
	/**
	 * Conta quantas ovelhas existem À volta de um quadrado cujas coordenadas são indicadas atraves dos parametros e devolve essa contagem
	 * @param xPretendido
	 * @param yPretendido
	 * @return
	 */
	private int contaOvelhasVolta(int xPretendido, int yPretendido)
	{
		int numOvelhas = 0;
		if(xPretendido == 0 && yPretendido == 0)
		{
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(xPretendido == 0 && yPretendido == 5)
		{
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(xPretendido == 5 && yPretendido == 0)
		{
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(xPretendido == 5 && yPretendido == 5)
		{
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(xPretendido == 0)
		{
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(yPretendido == 0)
		{
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(xPretendido == 5)
		{
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else if(yPretendido == 5)
		{
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		else
		{
			if(tb.getTabuleiro()[xPretendido + 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido - 1][yPretendido].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido + 1].hasOvelha())
			{
				numOvelhas++;
			}
			if(tb.getTabuleiro()[xPretendido][yPretendido - 1].hasOvelha())
			{
				numOvelhas++;
			}
		}
		return numOvelhas;
	}
	/**
	 * Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à direita cima baixo da ovelha ou 
	 * a esquerda cima e baixo da ovelha.
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_DCB_ECB(int xOvelha, int yOvelha, int numOvelha)
	{
		System.out.println(" Local Ovekha Verifica " + xOvelha + " Y " + yOvelha);
		//Tou no meio do tabuleiro (ovelha)
		if(yOvelha > 0 && yOvelha < 5)
		{
			//System.out.println("Meme no x > 0 e y < 5");
			if(tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso())
			{//vou para a posicao abaixo da ovelha
				preencheFactores(xOvelha,yOvelha - 1);
				if(posicaoRobotX == 0 && posicaoRobotY ==0 && jogada1 == true)
				{
					jogada1 = false;
					contaPassos(xOvelha, yOvelha -1, false);
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha

				}
				else if(contaPassos(xOvelha, yOvelha -1, true) <= 2 )
				{
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{//Se não consigo ir para baixo vou para cima
					preencheFactores(xOvelha,yOvelha + 1);
					if(contaPassos(xOvelha, yOvelha + 1, true) <= 2)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.BAIXO)
						{
							roda();
						}
						//Age sobre ovelha
						getMotorsStop();
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();
						//Fica a esperada jogada da ovelha
					}
					else
					{
						preencheFactores(xOvelha - 1,yOvelha - 1);
						if(contaPassos(xOvelha - 1, yOvelha - 1, true) <= 2)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.CIMA)
							{
								roda();
							}
							//Age sobre ovelha
							getMotorsStop();							
							while(true)
							{
								System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
								Button.waitForAnyPress();
								if(distancia_ovelha() < distanciaMax(1.80))
								{
									if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
									{
										posicaoOvelha1[0] = xOvelha - 1;
										posicaoOvelha1[1] = yOvelha;
										posicaoOvelha2[0] = xOvelha - 1;
										posicaoOvelha2[1] = yOvelha;
									}
									else if(numOvelha == 1)
									{
										posicaoOvelha1[0] = xOvelha - 1;
										posicaoOvelha1[1] = yOvelha;
									}
									else 
									{
										posicaoOvelha2[0] = xOvelha - 1;
										posicaoOvelha2[1] = yOvelha;
										
									}
									if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
									{
										grita();
										atualizaPosicaoOvelha(numOvelha, 1);
									}
									else
									{
										toca();
										atualizaPosicaoOvelha(numOvelha, 2);
									}
									meteNoCurral(numOvelha);
									break;
								}
							}
							//Fica a esperada jogada da ovelha
						}
						else
						{
							preencheFactores(xOvelha - 1,yOvelha + 1);
							if(contaPassos(xOvelha - 1, yOvelha + 1, true) <= 2)
							{
								aproximaOvelha();
								while(orientacao != Quadrado.BAIXO)
								{
									roda();
								}
								//Age sobre ovelha
								getMotorsStop();							
								while(true)
								{
									System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
									Button.waitForAnyPress();
									if(distancia_ovelha() < distanciaMax(1.80))
									{
										if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
										{
											posicaoOvelha1[0] = xOvelha - 1;
											posicaoOvelha1[1] = yOvelha;
											posicaoOvelha2[0] = xOvelha - 1;
											posicaoOvelha2[1] = yOvelha;
										}
										else if(numOvelha == 1)
										{
											posicaoOvelha1[0] = xOvelha - 1;
											posicaoOvelha1[1] = yOvelha;
										}
										else
										{
											posicaoOvelha2[0] = xOvelha - 1;
											posicaoOvelha2[1] = yOvelha;
										}
										if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
										{
											grita();
											atualizaPosicaoOvelha(numOvelha, 1);
										}
										else
										{
											toca();
											atualizaPosicaoOvelha(numOvelha, 2);
										}
										meteNoCurral(numOvelha);
										break;
									}
								}
								//Fica a esperada jogada da ovelha
							}
						}
					}

				}
			}

		}
		else if(yOvelha == 0)
		{
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				preencheFactores(xOvelha,yOvelha + 1);

				if(contaPassos(xOvelha, yOvelha + 1, false) < 100)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					System.out.println("Nao posso fazer nada! :(");
				}
			}
			else
			{
				preencheFactores(xOvelha,yOvelha + 1);

				if(contaPassos(xOvelha, yOvelha + 1, false)<=2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha - 1,yOvelha + 1);
					
					if(contaPassos(xOvelha - 1, yOvelha + 1, false)<=2)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.BAIXO)
						{
							roda();
						}
						//Age sobre ovelha
						getMotorsStop();
						while(true)
						{
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();
							if(distancia_ovelha() < distanciaMax(1.80))
							{
								if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
								{
									posicaoOvelha1[0] = xOvelha - 1;
									posicaoOvelha1[1] = yOvelha;
									posicaoOvelha2[0] = xOvelha - 1;
									posicaoOvelha2[1] = yOvelha;
								}
								else if(numOvelha == 1)
								{
									posicaoOvelha1[0] = xOvelha - 1;
									posicaoOvelha1[1] = yOvelha;
								}
								else
								{
									posicaoOvelha2[0] = xOvelha - 1;
									posicaoOvelha2[1] = yOvelha;
								}
								if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
								{
									grita();
									atualizaPosicaoOvelha(numOvelha, 1);
								}
								else
								{
									toca();
									atualizaPosicaoOvelha(numOvelha, 2);
								}
								meteNoCurral(numOvelha);
								break;
							}
						}
					}
				}
			}
		}
		else if(yOvelha == 5)
		{
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				preencheFactores(xOvelha,yOvelha - 1);
				if(contaPassos(xOvelha, yOvelha - 1, false) < 100)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					System.out.println("Nao posso fazer nada! :(");
				}
				
			}
			else
			{
				preencheFactores(xOvelha,yOvelha - 1);
				if(contaPassos(xOvelha, yOvelha - 1, false)<=2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha - 1,yOvelha - 1);

					if(contaPassos(xOvelha - 1, yOvelha - 1, false)<=2)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.CIMA)
						{
							roda();
						}
						//Age sobre ovelha
						getMotorsStop();
						while(true)
						{
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();
							if(distancia_ovelha() < distanciaMax(1.80))
							{
								if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
								{
									posicaoOvelha1[0] = xOvelha - 1;
									posicaoOvelha1[1] = yOvelha;
									posicaoOvelha2[0] = xOvelha - 1;
									posicaoOvelha2[1] = yOvelha;
								}
								else if(numOvelha == 1)
								{
									posicaoOvelha1[0] = xOvelha - 1;
									posicaoOvelha1[1] = yOvelha;
								}
								else
								{
									posicaoOvelha2[0] = xOvelha - 1;
									posicaoOvelha2[1] = yOvelha;
								}
								if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
								{
									grita();
									atualizaPosicaoOvelha(numOvelha, 1);
								}
								else
								{
									toca();
									atualizaPosicaoOvelha(numOvelha, 2);
								}
								meteNoCurral(numOvelha);
								break;
							}
						}
					}
				}
			}
			
		}
		//Se o de cima for maior fazer else aqui nunca deverá acontecer.
	}

	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à direita baixo esquerda da ovelha ou 
	 * a direita cima e esquerda da ovelha.
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_DBE_DCE(int xOvelha, int yOvelha, int numOvelha)
	{
		if(xOvelha > 0 && xOvelha < 5)
		{
			if(tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
			{
				preencheFactores(xOvelha - 1,yOvelha);
				if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
				{	
					jogada1 = false;
					//Se estiver na posicao 00 entao vai para a posicao a esquerda da ovelha sem contar passos
					contaPassos(xOvelha - 1, yOvelha, false);
					aproximaOvelha();
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else if (contaPassos(xOvelha - 1, yOvelha, true) <= 2)
				{
					//vai para a esquerda da ovelha se tiver de dar 2 ou menos pass

					aproximaOvelha();
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					//Se nao pode ir para a esquerda vai para a direita.
					preencheFactores(xOvelha + 1,yOvelha);
					if(contaPassos(xOvelha + 1, yOvelha, false) <= 2)
					{
						aproximaOvelha();

						while(orientacao != Quadrado.ESQUERDA)
						{
							roda();
						}
						//Age sobre ovelha
						getMotorsStop();
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();
						//Fica a esperada jogada da ovelha
					}

					else
					{
						preencheFactores(xOvelha - 1,yOvelha + 1);

						if(contaPassos(xOvelha - 1, yOvelha + 1, false)<=2)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.DIREITA)
							{
								roda();
							}
							//Age sobre ovelha
							getMotorsStop();
							while(true)
							{
								System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
								Button.waitForAnyPress();
								if(distancia_ovelha() < distanciaMax(1.80))
								{
									if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
									{
										posicaoOvelha1[0] = xOvelha;
										posicaoOvelha1[1] = yOvelha + 1;
										posicaoOvelha2[0] = xOvelha;
										posicaoOvelha2[1] = yOvelha + 1;
									}
									else if(numOvelha == 1)
									{
										posicaoOvelha1[0] = xOvelha;
										posicaoOvelha1[1] = yOvelha + 1;
									}
									else
									{
										posicaoOvelha2[0] = xOvelha;
										posicaoOvelha2[1] = yOvelha + 1;
									}
									if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
									{
										grita();
										atualizaPosicaoOvelha(numOvelha, 1);
									}
									else
									{
										toca();
										atualizaPosicaoOvelha(numOvelha, 2);
									}
									meteNoCurral(numOvelha);
									break;
								}
							}
						}
						else
						{
							preencheFactores(xOvelha + 1,yOvelha + 1);

							if(contaPassos(xOvelha + 1, yOvelha + 1, false)<=2)
							{
								aproximaOvelha();
								while(orientacao != Quadrado.ESQUERDA)
								{
									roda();
								}
								//Age sobre ovelha
								getMotorsStop();
								while(true)
								{
									System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
									Button.waitForAnyPress();
									if(distancia_ovelha() < distanciaMax(1.80))
									{
										if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
										{
											posicaoOvelha1[0] = xOvelha;
											posicaoOvelha1[1] = yOvelha + 1;
											posicaoOvelha2[0] = xOvelha;
											posicaoOvelha2[1] = yOvelha + 1;
										}
										else if(numOvelha == 1)
										{
											posicaoOvelha1[0] = xOvelha;
											posicaoOvelha1[1] = yOvelha + 1;
										}
										else
										{
											posicaoOvelha2[0] = xOvelha;
											posicaoOvelha2[1] = yOvelha + 1;
										}
										if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
										{
											grita();
											atualizaPosicaoOvelha(numOvelha, 1);
										}
										else
										{
											toca();
											atualizaPosicaoOvelha(numOvelha, 2);
										}
										meteNoCurral(numOvelha);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		else if(xOvelha == 0)
		{
			preencheFactores(xOvelha + 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{	
				jogada1 = false;
				//Se estiver na posicao 00 entao vai para a posicao a esquerda da ovelha sem contar passos
				contaPassos(xOvelha + 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha + 1, yOvelha, true) <= 2)
			{
				//vai para a esquerda da ovelha se tiver de dar 2 ou menos pass

				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha + 1,yOvelha - 1);
				contaPassos(xOvelha + 1, yOvelha - 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				while(true)
				{
				Button.waitForAnyPress();
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				if(distancia_ovelha() < distanciaMax(1.80))
				{
					if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
					{
						posicaoOvelha1[0] = xOvelha;
						posicaoOvelha1[1] = yOvelha- 1;
						posicaoOvelha2[0] = xOvelha;
						posicaoOvelha2[1] = yOvelha - 1;
					}
					else if(numOvelha == 1)
					{
						posicaoOvelha1[0] = xOvelha;
						posicaoOvelha1[1] = yOvelha - 1;
					}
					else
					{
						posicaoOvelha2[0] = xOvelha;
						posicaoOvelha2[1] = yOvelha - 1;
					}
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					meteNoCurral(numOvelha);
					break;
				}
				}
				//Fica a esperada jogada da ovelha
			}
		}
		else if(xOvelha == 5)
		{
			preencheFactores(xOvelha - 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{	
				jogada1 = false;
				//Se estiver na posicao 00 entao vai para a posicao a esquerda da ovelha sem contar passos
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				//vai para a esquerda da ovelha se tiver de dar 2 ou menos pass

				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha - 1,yOvelha - 1);
				contaPassos(xOvelha - 1, yOvelha - 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				while(true)
				{
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					if(distancia_ovelha() < distanciaMax(1.80))
					{
						if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
						{
							posicaoOvelha1[0] = xOvelha;
							posicaoOvelha1[1] = yOvelha - 1;
							posicaoOvelha2[0] = xOvelha;
							posicaoOvelha2[1] = yOvelha - 1;
						}
						else if(numOvelha == 1)
						{
							posicaoOvelha1[0] = xOvelha;
							posicaoOvelha1[1] = yOvelha - 1;
						}
						else
						{
							posicaoOvelha2[0] = xOvelha;
							posicaoOvelha2[1] = yOvelha - 1;
						}
						if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
						{
							grita();
							atualizaPosicaoOvelha(numOvelha, 1);
						}
						else
						{
							toca();
							atualizaPosicaoOvelha(numOvelha, 2);
						}
						meteNoCurral(numOvelha);
						break;
					}
				}
				//Fica a esperada jogada da ovelha
			}
		}
	}

	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à esquerda e em baixo
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_EB(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
		{
			preencheFactores(xOvelha,yOvelha + 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{	
				jogada1 = false;
				//Se estiver na posicao 00 entao vai para a posicao acima da ovelha sem contar passos
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha, yOvelha + 1, true) <= 2)
			{
				//vai para a cima da ovelha se tiver de dar 2 ou menos pass

				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				//Se nao pode ir para cima vai para a direita.
				preencheFactores(xOvelha + 1,yOvelha);
				contaPassos(xOvelha + 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
	}
	
	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à esquerda e em cima
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_EC(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
		{
			preencheFactores(xOvelha,yOvelha - 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha, yOvelha - 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha, yOvelha - 1, true) <= 2)
			{
				//vai para a cima da ovelha se tiver de dar 2 ou menos pass	
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				//Se nao pode ir para cima vai para a direita.
				preencheFactores(xOvelha + 1,yOvelha);
				contaPassos(xOvelha + 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
	}

	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à direita e acima da ovelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_DC(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso())
		{
			preencheFactores(xOvelha - 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				//Colca-se na posição A esquerda
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				//Verifica se pode ir para a esquerda em menos de 2 passos
				aproximaOvelha();	
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				//Vai para o quadrado abaixo.
				preencheFactores(xOvelha,yOvelha - 1);
				contaPassos(xOvelha, yOvelha - 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
	}
	
	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à direita e em baixo da ovelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_DB(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso())
		{
			preencheFactores(xOvelha - 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				//vou para o quadrado esquerda
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();	
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				//vejo se posso ir para o quadrado a esquerda em menos de 2 passos.
				aproximaOvelha();	
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha,yOvelha + 1);
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				toca();
				atualizaPosicaoOvelha(numOvelha, 2);
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
	}

	//----------------------------------------------------------------------------------------	


	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à esquerda da ovelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_E(int xOvelha, int yOvelha, int numOvelha)
	{
		//BC && BD

		if(tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() && 
				tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
		{
			preencheFactores(xOvelha,yOvelha - 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				//Marca o caminho

				//Raciocinio
				contaPassos(xOvelha, yOvelha  - 1, false);
				//Anda
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha, yOvelha  - 1, true) <= 2)
			{
				//Anda
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				auxEsq(xOvelha, yOvelha, numOvelha);
			}

		}
		else
		{
			auxEsq(xOvelha, yOvelha, numOvelha);
		}

	}
	
	/**
	 * Método auxiliar ao método meteNoCurral_E que contém o conjunto de instruções que o robot deve executar quando não pode dirigir-se para baixo
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void auxEsq(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso()) 
		{
			preencheFactores(xOvelha,yOvelha + 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{			
				jogada1 = false;
				//Raciocinio
				contaPassos(xOvelha, yOvelha  + 1, false);
				//Anda
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha, yOvelha  + 1, true) <= 2)
			{
				//Anda
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha + 1,yOvelha);
				if(contaPassos(xOvelha + 1, yOvelha, true)<=2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha,yOvelha + 1);
					contaPassos(xOvelha, yOvelha + 1, false);
					aproximaOvelha();
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
			}
		}
	}
	
	/**
	 *  Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à baixo da ovelha.
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_B(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() && 
				tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
		{
			//Esquerda
			preencheFactores(xOvelha - 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				auxBai(xOvelha, yOvelha, numOvelha);
			}
		}
		else
		{
			auxBai(xOvelha, yOvelha, numOvelha);
		}
	}

	/**
	 * Método auxiliar ao método meteNoCurral_B que contém o conjunto de instruções que o robot deve executar quando não pode dirigir-se para esquerda
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void auxBai(int xOvelha, int yOvelha, int numOvelha)
	{//C < Dir

		if(tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso() )
		{
			preencheFactores(xOvelha,yOvelha + 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha, yOvelha + 1, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha + 1,yOvelha);
				if(contaPassos(xOvelha + 1, yOvelha, true)<=2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha,yOvelha + 1);
					contaPassos(xOvelha, yOvelha + 1, false);
					aproximaOvelha();
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
			}
		}
	}

	/**
	 * Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras à direita da ovelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_D(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() && 
				tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso())
		{
			preencheFactores(xOvelha,yOvelha - 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha, yOvelha - 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}
				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if (contaPassos(xOvelha, yOvelha - 1, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				auxDir(xOvelha, yOvelha, numOvelha);
			}
		}
		else
		{
			auxDir(xOvelha, yOvelha, numOvelha);
		}
	}

	/**
	 *  Método auxiliar ao método meteNoCurral_D que contém o conjunto de instruções que o robot deve executar quando não pode dirigir-se para baixo
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void auxDir(int xOvelha, int yOvelha, int numOvelha)
	{

		if(tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso() <= tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso())
		{
			preencheFactores(xOvelha,yOvelha + 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha, yOvelha + 1, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha - 1,yOvelha);
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha

			}
		}
		else
		{
			preencheFactores(xOvelha - 1,yOvelha);
			if(contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha,yOvelha + 1);
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
	}

	/**
	 * Metodo auxiliar ao metodo meteNoCurral que trata as situacoes em que existem barreiras em cima da ovelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void meteNoCurral_C(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() && 
				tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso())
		{
			preencheFactores(xOvelha - 1,yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha - 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				auxCima(xOvelha, yOvelha, numOvelha);
			}
		}
		else
		{
			auxCima(xOvelha, yOvelha, numOvelha);
		}
	}

	/**
	 * Método auxiliar ao método meteNoCurral_C que contém o conjunto de instruções que o robot deve executar quando não pode dirigir-se para esquerda
	 * @param xOvelha
	 * @param yOvelha
	 * @param numOvelha
	 */
	private void auxCima(int xOvelha, int yOvelha, int numOvelha)
	{
		if(tb.getTabuleiro()[xOvelha][yOvelha - 1].getPeso() <= tb.getTabuleiro()[xOvelha + 1][yOvelha ].getPeso())
		{
			preencheFactores(xOvelha,yOvelha - 1);
			if(posicaoRobotX == 0 && posicaoRobotY == 0 && jogada1 == true)
			{
				jogada1 = false;
				contaPassos(xOvelha,yOvelha - 1, false) ;
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(contaPassos(xOvelha,yOvelha - 1, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.CIMA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha + 1,yOvelha);
				if(contaPassos(xOvelha + 1, yOvelha, true) <= 2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha,yOvelha - 1);
					contaPassos(xOvelha,yOvelha - 1, false) ;
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
			}
		}
	}

	/**
	 * Método responsável por colocar as ovelhas no curral, recebe um inteiro que indica se o robot está a tentar colocar a primeira ou a segunda ovelha.
	 * @param numOvelha
	 */
	public void meteNoCurral(int numOvelha)
	{
		int xOvelha;
		int yOvelha;
		
		if(numOvelha == 1)
		{
			xOvelha = posicaoOvelha1[0];
			yOvelha = posicaoOvelha1[1];
		}
		else
		{
			xOvelha = posicaoOvelha2[0];
			yOvelha = posicaoOvelha2[1];
		}
		boolean var = false;
		while(!(xOvelha == 5 && yOvelha == 5))
		{
			var = false;
			System.out.println("Local Ovelha X " + xOvelha + " Y " + yOvelha);
			if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA]
					|| tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
			{
				if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					//System.out.println("TEM BARR DCB");
					meteNoCurral_DCB_ECB(xOvelha,yOvelha, numOvelha);
					var = true;		
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA])
				{
					meteNoCurral_DCB_ECB(xOvelha,yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA])
				{
					meteNoCurral_DBE_DCE(xOvelha,yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA])
				{
					meteNoCurral_DBE_DCE(xOvelha,yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					meteNoCurral_EB(xOvelha, yOvelha, numOvelha);
					var = true;
				} 
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA])
				{
					meteNoCurral_EC(xOvelha, yOvelha, numOvelha);
					var = true;	
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA])
				{
					meteNoCurral_DC(xOvelha, yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA] && tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					meteNoCurral_DB(xOvelha,yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA]
						&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA]
								&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA]
										&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					meteNoCurral_E(xOvelha, yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO]
						&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA]
								&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA]
										&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA])
				{
					meteNoCurral_B(xOvelha, yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA]
						&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA]
								&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA]
										&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					meteNoCurral_D(xOvelha, yOvelha, numOvelha);
					var = true;
				}
				else if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA]
						&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA]
								&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA]
										&& !tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
				{
					meteNoCurral_C(xOvelha, yOvelha, numOvelha);
					var = true;
				}
			}

			//Seleciona o quadrado com maior peso.
			if(var == false)
			{
				maiorPeso(xOvelha, yOvelha, numOvelha);
			}
			if(numOvelha == 1)
			{
				xOvelha = posicaoOvelha1[0];
				yOvelha = posicaoOvelha1[1];
			}
			else
			{
				xOvelha = posicaoOvelha2[0];
				yOvelha = posicaoOvelha2[1];
			}
			limpaFactores();
		}
		if(posicaoOvelha1[0] == 5 && posicaoOvelha2[0] == 5 && posicaoOvelha1[1] == 5 && posicaoOvelha2[1] == 5)
		{
			Sound.systemSound(true, 2);
		}
		else
		{
			preencheFactores(0,0);
			contaPassos(0,0, false);
			aproximaOvelha();
			getMotorsStop();
			System.out.println("Voltei ao (0,0) prima em qualquer botao para continuar.");
			Button.waitForAnyPress();
			while(orientacao != Quadrado.CIMA)
			{
				roda();
			}
			getMotorsStop();
			System.out.println("Vou me dirigir para a segunda ovelha prima em qualquer botao para continuar.");
			Button.waitForAnyPress();
			jogada1 = true;
			meteNoCurral(2);
		}
		
	}
	
	/**
	 * Método que apaga da matriz os factores usados para o cálculo do caminho colocando-os a 0
	 * Necessário sempre que se preenche a matriz com novos factores
	 */
	public void limpaFactores()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				tb.getTabuleiro()[i][j].setFactorEscolha(0);
			}
		}
	}

	/**
	 * Método utilizado como debug sempre que se pretende testar casos sem a volta de reconhecimento
	 * Aqui colocamos as posições iniciais da ovelha e as barreiras
	 */
	public void debugFactores()
	{
		tb.getTabuleiro()[5][0].setOvelha(true);
		//tb.getTabuleiro()[5][5].setOvelha(true);
		  
		tb.getTabuleiro()[5][2].alteraBarreira(Quadrado.CIMA);
		tb.getTabuleiro()[5][3].alteraBarreira(Quadrado.BAIXO);
		
		/*tb.getTabuleiro()[1][5].alteraBarreira(Quadrado.DIREITA);
		tb.getTabuleiro()[2][5].alteraBarreira(Quadrado.ESQUERDA);
		
		tb.getTabuleiro()[2][3].alteraBarreira(Quadrado.CIMA);
		tb.getTabuleiro()[2][4].alteraBarreira(Quadrado.BAIXO);
		
		tb.getTabuleiro()[4][5].alteraBarreira(Quadrado.DIREITA);
		tb.getTabuleiro()[5][5].alteraBarreira(Quadrado.ESQUERDA);*/
		
		posicaoOvelha();
	}

	/**
	 * Imprime num barreiras para efeitos de debug
	 */
	public void imprimeNumBarrreiras()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				System.out.println("X " + i + " Y " + j + " Num Barreiras " + tb.getTabuleiro()[i][j].getNumBarreiras());
			}
		}
	}
	
	/**
	 * Limpa os quadrados visitados
	 */
	public void limpaMatriz()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				tb.getTabuleiro()[i][j].setVisitado(false);
			}
		}
	}

	/**
	 * Imprime os pesos usados para a decisão da posição onde o robot deve se posicionar para efeitos de debug
	 */
	public void imprimeNumPesos()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				System.out.println("X " + i + " Y " + j + " Peso " + tb.getTabuleiro()[i][j].getPeso());
			}
		}
	}

	/**
	 * Método que remove todas as migalhas e coloca todos os quadrados como por visitar
	 * Necessário após o reconhecimento do tabuleiro
	 */
	public void limpaVisitadosMigalhas()
	{
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				tb.getTabuleiro()[i][j].setVisitado(false);
				tb.getTabuleiro()[i][j].setMigalha(0);
			}
		}
	}
	
	/**
	 * Verifica se voltei ao sitio onde comecei analisa os quadrados em volta e se tudo estiver visitado para o ciclo.
	 */
	private boolean quebraCiclo(int xRobotBack,int yRobotBack,int orientacaoBack)
	{
		if(posicaoRobotX == xRobotBack && posicaoRobotY == yRobotBack)
		{
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				if((tb.getTabuleiro()[0][1].isVisitado() || tb.getTabuleiro()[0][0].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[0][1].hasOvelha())&& (tb.getTabuleiro()[1][0].isVisitado() ||tb.getTabuleiro()[0][0].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[1][0].hasOvelha()))
				{
					return true;
				}
			}
			else if (posicaoRobotX == 0 && posicaoRobotY == 5)
			{
				if((tb.getTabuleiro()[0][4].isVisitado() || tb.getTabuleiro()[0][5].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[0][4].hasOvelha())&& (tb.getTabuleiro()[1][5].isVisitado()||tb.getTabuleiro()[0][5].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[1][5].hasOvelha()))
				{
					return true;
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 0 )
			{
				if((tb.getTabuleiro()[4][0].isVisitado() || tb.getTabuleiro()[5][0].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[4][0].hasOvelha())&& (tb.getTabuleiro()[5][1].isVisitado() || tb.getTabuleiro()[5][0].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[5][1].hasOvelha()))
				{
					return true;
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 5)
			{
				if((tb.getTabuleiro()[4][5].isVisitado() || tb.getTabuleiro()[5][5].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[4][5].hasOvelha()) && (tb.getTabuleiro()[5][4].isVisitado() || tb.getTabuleiro()[5][5].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[5][4].hasOvelha()))
				{
					return true;
				}
			}
			else if(posicaoRobotX == 0)
			{
				if((tb.getTabuleiro()[0][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[0][posicaoRobotY + 1].hasOvelha()) && 
				   (tb.getTabuleiro()[0][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[0][posicaoRobotY - 1].hasOvelha()) &&
				   (tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado()|| tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
				   )
				{
					return true;
				}
			}
			else if(posicaoRobotY == 0)
			{
				if((tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha()) && 
				   (tb.getTabuleiro()[posicaoRobotX - 1][0].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[posicaoRobotX - 1][0].hasOvelha())&&
				   (tb.getTabuleiro()[posicaoRobotX + 1][0].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[posicaoRobotX + 1][0].hasOvelha())
				   )
				{
							return true;
				}
			}
			else if(posicaoRobotX == 5)
			{
				if((tb.getTabuleiro()[5][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[5][posicaoRobotY + 1].hasOvelha())&& 
				   (tb.getTabuleiro()[5][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[5][posicaoRobotY - 1].hasOvelha())&&
				   (tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
				   )
				{
							return true;
				}
			}
			else if(posicaoRobotY == 5)
			{
				if((tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())&& 
		     	   (tb.getTabuleiro()[posicaoRobotX + 1][5].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[posicaoRobotX + 1][5].hasOvelha()) &&
				   (tb.getTabuleiro()[posicaoRobotX - 1][5].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[posicaoRobotX - 1][5].hasOvelha()) 
						   )
				{
									return true;
				}
			}
			else
			{
				if((tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha()) && 
				   (tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha()) &&
				   (tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] || tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) && 
				   (tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] || tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
						)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Método que simula todos os possiveis caminhos que o robot pode seguir ate chegar ou pé da ovelha. Pensamento do robot
	 * Faz sem andar.
	 * (Busca gulosa)
	 */
	public int contaPassos(int xNovaPos, int yNovaPos, boolean tentaDesviar)
	{
		int xRobotBack = posicaoRobotX;
		int yRobotBack = posicaoRobotY;
		int orientacaoBack = orientacao;
		limpaVisitadosMigalhas();
		int contaPassos = 0;
		novaMigalha = 1;
		System.out.println("Quero ir para o " + xNovaPos + " " + yNovaPos);
		imprimeFactores();
		System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
		int [] factores;
		while(true)
		{
			if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
			{
				break;
			}
			if(quebraCiclo(xRobotBack, yRobotBack, orientacaoBack))
			{
				posicaoRobotX = xRobotBack;
				posicaoRobotY = yRobotBack;
				orientacao = orientacaoBack;
				return 100;
			}
			tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				factores = new int[2];
				int controla = 0;
				//verificar quadrado acima e a direita
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				//Vou para cima
				else if(factores[1] < factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha()&& !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}


					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[0] == factores[1] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}

			}
			else if(posicaoRobotX == 0 && posicaoRobotY == 5)
			{
				factores = new int[2];
				int controla = 0;
				//verificar quadrado abaixo e a direita
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()&& !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}


					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				//Vou para baixo
				else if(factores[0] > factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.BAIXO;
					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}


					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				else if(factores[0] == factores[1] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
			else if (posicaoRobotX == 5 && posicaoRobotY == 5)
			{
				factores = new int[2];
				int controla = 0;
				//verificar quadrado abaico e a ESQUERDA
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());


					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				//Vou para cima
				else if(factores[1] < factores[0] && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					orientacao = Quadrado.BAIXO;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				else if(factores[0] == factores[1] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 0)
			{
				factores = new int[2];
				int controla = 0;
				//vou verificar os quadrados acima e a esquerda
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				//Vou para cima
				else if(factores[0] > factores[1] && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha()&& !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				else if(factores[0] == factores[1] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
			else if(posicaoRobotX == 0)
			{
				factores = new int[3];
				int controla = 0;
				//verifica quadraos acima abaixo e a direita.
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && factores[0] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				//Vou para cima
				else if(factores[0] > factores[1] && factores[1] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] &&!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.BAIXO;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[2] < factores[1] && factores[2] < factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				else if(factores[0] == factores[1] && factores[0] <= factores[2] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[0] && factores[2] <= factores[1] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha()) 
						|| (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
			else if(posicaoRobotX == 5)
			{
				factores = new int[3];
				int controla = 0;
				//verifica quadrados acima abaixo e a esquerda.
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && factores[0] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				//Vou para cima
				else if(factores[1] < factores[0] && factores[1] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.BAIXO;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[2] < factores[1] && factores[2] < factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[0] == factores[1] && factores[0] <= factores[2] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[0] && factores[2] <= factores[1] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha()) 
						|| (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}

			}
			else if(posicaoRobotY == 0)
			{
				factores = new int[3];
				int controla = 0;
				//Verifica quadrados acima diretira esquerda.
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && factores[0] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();					
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				//Vou para ESQUERDA
				else if(factores[1] < factores[0] && factores[1] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();					
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[2] < factores[1] && factores[2] < factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA]  && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[0] == factores[1] && factores[0] <= factores[2] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[0] && factores[2] <= factores[1] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) 
						|| (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}

			}
			else if(posicaoRobotY == 5)
			{
				factores = new int[3];
				int controla = 0;
				//Verifica quadrados abaixo esquerda  direita. 
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();

				//Vou para direita
				if(factores[0] < factores[1] && factores[0] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				//Vou para cima
				else if(factores[1] < factores[0] && factores[1] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.BAIXO;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				else if(factores[2] < factores[1] && factores[2] < factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();
				}
				else if(factores[0] == factores[1] && factores[0] <= factores[2] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[0] && factores[2] <= factores[1] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) 
						|| (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
			else
			{
				factores = new int[4];
				int controla = 0;
				//Verificação dos quatro quadrados caso o robo esteja no meio do tabueleiro
				factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();
				controla++;
				factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();


				//Vou para direita
				if(factores[0] < factores[1] && factores[0] < factores[2] && factores[0] < factores[3]&& !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.DIREITA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
					if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}
					decideVoltarAtras();

				}
				//Vou para BAIXO
				else if(factores[1] < factores[0] && factores[1] <factores[2] && factores[1] < factores[3] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.BAIXO;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}

					decideVoltarAtras();

				}
				else if(factores[2] < factores[0] && factores[2] < factores[1] && factores[2] < factores[3] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.CIMA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}

					decideVoltarAtras();

				}
				else if(factores[3] < factores[0] && factores[3] < factores[1] && factores[3] < factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha() && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{
					tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
					orientacao = Quadrado.ESQUERDA;

					andou = true;
					contaPassos++;
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")")
					;if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
					{
						novaMigalha++;
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
					}
					System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

					if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
					{
						break;
					}

					decideVoltarAtras();



				}
				else if(factores[0] == factores[1] && factores[0] <= factores[2] && factores[0] <= factores[3] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[1] == factores[3] && factores[1] <= factores[0] && factores[1] <= factores[2] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[3] && factores[2] <= factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[0] && factores[2] <= factores[1] && factores[2] <= factores[3] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[3] == factores[0] && factores[2] <= factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha()) || (!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha()) ))
				{
					if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else if(factores[2] == factores[1] && factores[2] <= factores[1] && factores[2] <= factores[0] && ((!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha()) 
						|| (!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())))
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}

						decideVoltarAtras();
					}
				}
				else
				{
					if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.CIMA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.DIREITA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.BAIXO;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());

						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else if(!tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].hasOvelha())
					{
						tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
						orientacao = Quadrado.ESQUERDA;

						andou = true;
						contaPassos++;
						atualizaPosicao();
						System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
						if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() == 0)
						{
							novaMigalha++;
							tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(novaMigalha);
						}
						System.out.println("Migalha " + tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha());
						if(posicaoRobotX == xNovaPos && posicaoRobotY == yNovaPos)
						{
							break;
						}
						decideVoltarAtras();
					}
					else
					{
						//Volta para a maior migalha
						voltaAtrasSim(true);
					}
				}
			}
		}
		posicaoRobotX = xRobotBack;
		posicaoRobotY = yRobotBack;
		orientacao = orientacaoBack;
		return novaMigalha - 1;
	}
	
	/**
	 * Verifica se o novo nodo para o quadrado aonde foi tem um fator 
	 * pior do que o que o robot estava antes e se for volta para trás
	 */
	private void decideVoltarAtras()
	{
		int[] factores;
		//Procura a  migalha maior A volta
		int [] posicMigMax = new int[2];
		posicMigMax = encontraMigalhaMaior();
		int posicaoXMigMax = posicMigMax[0];
		int posicaoYMigMax = posicMigMax[1];

		if(posicaoRobotX == 0 && posicaoRobotY == 0)
		{
			factores = new int[2];
			int controla = 0;
			//verificar quadrado acima e a direita
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();
			if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1])
			{
				if(factores[0] < factores[1])
				{
					voltaAtrasSim(true);
				}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY + 1 && factores[1] <= factores[0])
			{
				if(factores[1] < factores[0])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}
		}
		else if(posicaoRobotX == 0 && posicaoRobotY == 5)
		{
			factores = new int[2];
			int controla = 0;
			//verificar quadrado abaixo e a direita
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1])
			{
				if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					voltaAtrasSim(true);
				}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0])
			{
				if(factores[1] < factores[0])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}

		}
		else if (posicaoRobotX == 5 && posicaoRobotY == 5)
		{
			factores = new int[2];
			int controla = 0;
			//verificar quadrado abaico e a ESQUERDA
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			if(posicaoXMigMax == posicaoRobotX - 1 && factores[0] <= factores[1])
			{
				if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{
					voltaAtrasSim(true);
				}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0])
			{
				if(factores[1] < factores[0])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}

		}
		else if(posicaoRobotX == 5 && posicaoRobotY == 0)
		{
			factores = new int[2];
			int controla = 0;
			//vou verificar os quadrados acima e a esquerda
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();
			if(posicaoXMigMax == posicaoRobotX - 1 && factores[0] <= factores[1])
			{
				if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{
					voltaAtrasSim(true);
				}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY + 1 && factores[1] <= factores[0])
			{
				if(factores[1] < factores[0])
				{
					voltaAtrasSim(true);
				}
				if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}

		}
		else if(posicaoRobotX == 0)
		{
			factores = new int[3];
			int controla = 0;
			//verifica quadraos acima abaixo e a direita.
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

			if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1] && factores[0] <= factores[2])
			{
				if(factores[0] < factores[1] && factores[0] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[0] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0] && factores[1] <= factores[2])
			{
				if(factores[1] < factores[0] && factores[1] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[1] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY + 1 && factores[2] <= factores[0] && factores[2] <= factores[1])
			{
				if(factores[2] < factores[0] && factores[2] < factores[1])
				{
					voltaAtrasSim(true);
				}
				else if(factores[2] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}
		}
		else if(posicaoRobotX == 5)
		{
			factores = new int[3];
			int controla = 0;
			//verifica quadrados acima abaixo e a esquerda.
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

			if(posicaoXMigMax == posicaoRobotX - 1 && factores[0] <= factores[1] && factores[0] <= factores[2])
			{
				if(factores[0] < factores[1] && factores[0] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[0] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0] && factores[1] <= factores[2])
			{
				if(factores[1] < factores[0] && factores[1] < factores[2])
				{}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else if(factores[1] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY + 1 && factores[2] <= factores[0] && factores[2] <= factores[1])
			{
				if(factores[2] < factores[0] && factores[2] < factores[1])
				{
					voltaAtrasSim(true);
				}
				else if(factores[2] == factores[0] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}
		}
		else if(posicaoRobotY == 0)
		{
			factores = new int[3];
			int controla = 0;
			//Verifica quadrados acima diretira esquerda.
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();

			if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1] && factores[0] <= factores[2])
			{
				if(factores[0] < factores[1] && factores[0] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[0] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoXMigMax == posicaoRobotX - 1 && factores[1] <= factores[0] && factores[1] <= factores[2])
			{
				if(factores[1] < factores[0] && factores[1] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[1] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1] .isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY + 1 && factores[2] <= factores[0] && factores[2] <= factores[1])
			{
				if(factores[2] < factores[0] && factores[2] < factores[1])
				{
					voltaAtrasSim(true);
				}
				else if(factores[2] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[1] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}


		}
		else if(posicaoRobotY == 5)
		{
			factores = new int[3];
			int controla = 0;
			//Verifica quadrados abaixo esquerda  direita. 
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();
			if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1] && factores[0] <= factores[2])
			{
				if(factores[0] < factores[1] && factores[0] < factores[2])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else if(factores[0] == factores[2] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0] && factores[1] <= factores[2])
			{
				if(factores[1] < factores[0] && factores[1] < factores[2])
				{}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[1] == factores[2] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[2] <= factores[0] && factores[2] <= factores[1])
			{
				if(factores[2] < factores[0] && factores[2] < factores[1])
				{
					voltaAtrasSim(true);
				}
				else if(factores[2] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}
			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}
		}
		else
		{
			factores = new int[4];
			int controla = 0;
			//Verificação dos quatro quadrados caso o robo esteja no meio do tabueleiro
			factores[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getFactorEscolha();
			controla++;
			factores[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getFactorEscolha();

			if(posicaoYMigMax == posicaoRobotY + 1 && factores[2] <= factores[0] && factores[2] <= factores[1] && factores[2] <= factores[3])
			{
				if(factores[2] < factores[0] && factores[2] < factores[1] && factores[2] < factores[3])
				{
					voltaAtrasSim(true);
				}
				else if(factores[2] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[3] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[2] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}

			}
			else if(posicaoXMigMax == posicaoRobotX - 1 && factores[3] <= factores[1] && factores[3] <= factores[2] && factores[3] <= factores[0])
			{

				if(factores[3] < factores[1] && factores[3] < factores[2] && factores[3] < factores[0])
				{
					voltaAtrasSim(true);
				}
				else if(factores[3] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[3] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else if(factores[3] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}

			}
			else if(posicaoXMigMax == posicaoRobotX + 1 && factores[0] <= factores[1] && factores[0] <= factores[2] && factores[0] <= factores[3])
			{

				if(factores[0] < factores[1] && factores[0] < factores[2] && factores[0] < factores[3])
				{
					voltaAtrasSim(true);
				}
				else if(factores[0] == factores[1] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado())
				{}
				else if(factores[0] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else if(factores[0] == factores[3] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}

			}
			else if(posicaoYMigMax == posicaoRobotY - 1 && factores[1] <= factores[0] && factores[1] <= factores[2] && factores[1] <= factores[3])
			{

				if(factores[1] < factores[0] && factores[1] < factores[2] && factores[1] < factores[3])
				{
					voltaAtrasSim(true);
				}
				else if(factores[1] == factores[0] && !tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado())
				{}
				else if(factores[1] == factores[3] && !tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY ].isVisitado())
				{}
				else if(factores[1] == factores[2] && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado())
				{}
				else
				{
					voltaAtrasSim(true);
				}

			}
			else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getNumBarreiras() == 3)
			{
				voltaAtrasSim(true);
			}
		}



	}

	/**
	 * Volta atrás durante a busca gulosa, nas situações em que fica preso.
	 * @param bloqueado
	 * @return
	 */
	private boolean voltaAtrasSim(boolean bloqueado)
	{
		int []posicMigMax  = new int[2];
		posicMigMax = encontraMigalhaMaior();
		int posicaoXMigMax = posicMigMax[0];
		int posicaoYMigMax = posicMigMax[1];
		System.out.println("Pos Migalha("+posicaoXMigMax+" "+posicaoYMigMax+ ")");
		if(posicaoRobotX > posicaoXMigMax)
		{
			//roda para orientacao esquerda
			orientacao = Quadrado.ESQUERDA;
		}
		else if(posicaoRobotX < posicaoXMigMax)
		{
			//roda para orientacao direita
			orientacao = Quadrado.DIREITA;
		}
		else if(posicaoRobotY > posicaoYMigMax)
		{
			//roda para orientacao baixo
			orientacao = Quadrado.BAIXO;
		}
		else
		{
			//roda para orientacao cima
			orientacao = Quadrado.CIMA;
		}
		tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setVisitado(true);
		tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].setMigalha(0);
		
		novaMigalha = novaMigalha-1;
		andou = true;
		atualizaPosicao();
		System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
		return false;
		//}
	}
	
	/**
	 * Método que encontra a maior migalha em volta de onde o robor se encontra.
	 * (Protegido os index out of bounds todos.)
	 * @return
	 */
	private int[] encontraMigalhaMaior()
	{

		int [] migalhas;
		int max = 0;
		int posicaoXMigMax = 0;
		int posicaoYMigMax = 0;
		System.out.println("Posicao Robot" + posicaoRobotX + " " + posicaoRobotY);
		if(posicaoRobotX == 0 && posicaoRobotY == 0)
		{
			migalhas = new int[2];
			int controla = 0;
			//verificar quadrado acima e a direita
			if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}

		}
		else if(posicaoRobotX == 0 && posicaoRobotY == 5)
		{
			migalhas = new int[2];
			int controla = 0;
			//verificar abaixo direita
			if( tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;
			}
		}
		else if (posicaoRobotX == 5 && posicaoRobotY == 5)
		{
			migalhas = new int[2];
			int controla = 0;
			//vou verificar quadrafdos abaixo e a esquerda
			if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;
			}
		}
		else if(posicaoRobotX == 5 && posicaoRobotY == 0)
		{
			migalhas = new int[2];
			int controla = 0;
			//vou verificar os quadrados acima e a esquerda 
			if( tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}
		}
		else if(posicaoRobotX == 0)
		{
			migalhas = new int[3];
			int controla = 0;
			//verifica quadraos acima abaixo e a direita.
			if( tb.getTabuleiro()[posicaoRobotX +1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX +1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;
			}
		}
		else if(posicaoRobotX == 5)
		{
			migalhas = new int[3];
			int controla = 0;
			//verifica quadrados acima abaixo e a esquerda.
			if( tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY +1 ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}
		}
		else if(posicaoRobotY == 0)
		{
			migalhas = new int[3];
			int controla = 0;
			//Verifica quadrados acima diretira esquerda.
			if( tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
				controla++;
			}
		}
		else if(posicaoRobotY == 5)
		{
			migalhas = new int[3];
			int controla = 0;
			//Verifica quadrados abaixo esquerda  direita. 
			if( tb.getTabuleiro()[posicaoRobotX ][posicaoRobotY - 1].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;

			}
			if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
				controla++;
			}if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY ].isVisitado() || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
				controla++;
			}
		}
		else
		{
			migalhas = new int[4];
			int controla = 0;
			//Verificação dos quatro quadrados caso o robo esteja no meio do tabueleiro
			if((posicaoRobotX > 0 && tb.getTabuleiro()[posicaoRobotX -1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if((posicaoRobotX <5 && tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha();
				controla++;
			}
			if((posicaoRobotY > 0 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY -1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha();
				controla++;
			}
			if((posicaoRobotY < 5 && tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].isVisitado()) || tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
			{
				migalhas[controla]=tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha();
				controla++;
			}
		}
		max = migalhas[0];
		for(int i = 0; i < migalhas.length; i++)
		{
			if(migalhas[i]>max)
			{
				max = migalhas[i];
			}
		}
		boolean checkfordentro = false;
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				if(tb.getTabuleiro()[i][j].getMigalha() == max)
				{
					checkfordentro =true;
					posicaoXMigMax = i;
					posicaoYMigMax = j;
					break;
				}
			}
			if(checkfordentro == true)
			{
				break;
			}
		}
		int []posicaoMigM = new int[2];
		posicaoMigM[0] = posicaoXMigMax;
		posicaoMigM[1] = posicaoYMigMax;
		return posicaoMigM;
	}
	
	/**
	 * Metodo que encontra o valor da migalha colocada pelo robot  no tabuleiro todo.
	 * @return
	 */
	private int encontraMaiorMig()
	{
		int migMax = 0;

		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				if(tb.getTabuleiro()[i][j].getMigalha() > migMax)
				{
					migMax = tb.getTabuleiro()[i][j].getMigalha();
				}
			}
		}
		return migMax;
	}

	/**
	 * Método que leva o robot para um aposicao adjacente a ovelha
	 * (Falo andar.)
	 */
	public void aproximaOvelha()
	{
		int migalhaMax = encontraMaiorMig();

		while(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() < migalhaMax)
		{
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotX == 0 && posicaoRobotY == 5)
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 0)
			{
				if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotX == 5 && posicaoRobotY == 5)
			{
				if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotX == 0)
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotX == 5)
			{
				if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotY == 0)
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else if(posicaoRobotY == 5)
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				else if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
			else
			{
				if(tb.getTabuleiro()[posicaoRobotX + 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.DIREITA])
				{
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				if(tb.getTabuleiro()[posicaoRobotX - 1][posicaoRobotY].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.ESQUERDA])
				{
					while(orientacao != Quadrado.ESQUERDA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY + 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.CIMA])
				{
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
				if(tb.getTabuleiro()[posicaoRobotX][posicaoRobotY - 1].getMigalha() > tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getMigalha() && !tb.getTabuleiro()[posicaoRobotX][posicaoRobotY].getBarreiraL()[Quadrado.BAIXO])
				{
					while(orientacao != Quadrado.BAIXO)
					{
						roda();
					}
					frente(tempoAndaFrente);
					atualizaPosicao();
					System.out.println("(" + posicaoRobotX + " " + posicaoRobotY + ")");
				}
			}
		}
	}

	/**
	 * Verifica todos os quadrados a volta da ovelha e posiciona-se de maneira vantajosa.
	 * Recebe as coordenadas da ovelha que procura
	 */
	private void maiorPeso(int xOvelha, int yOvelha, int numOvelha)
	{
		//int xOvelha = posicaoOvelha1[0];
		//int yOvelha = posicaoOvelha1[1];

		int esq = 0, dir = 0, cima = 0, baixo = 0;

		if(xOvelha == 0 || xOvelha == 5 || yOvelha == 0 || yOvelha == 5){
			if(xOvelha == 0){
				dir = tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso();
				cima = tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso();
				baixo = tb.getTabuleiro()[xOvelha][yOvelha -1].getPeso();
			}
			if(xOvelha == 5){
				esq = tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso();
				cima = tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso();
				baixo = tb.getTabuleiro()[xOvelha][yOvelha -1].getPeso();
			}
			if(yOvelha == 0){
				esq = tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso();
				dir = tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso();
				cima = tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso();
			}
			if(yOvelha == 5){
				dir = tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso();
				esq = tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso();
				baixo = tb.getTabuleiro()[xOvelha][yOvelha -1].getPeso();
			}
		}
		else{
			esq = tb.getTabuleiro()[xOvelha - 1][yOvelha].getPeso();
			dir = tb.getTabuleiro()[xOvelha + 1][yOvelha].getPeso();
			cima = tb.getTabuleiro()[xOvelha][yOvelha + 1].getPeso();
			baixo = tb.getTabuleiro()[xOvelha][yOvelha -1].getPeso();
		}

		int maximos[] = checkMax(esq, dir, cima, baixo);

		if(maximos[4] == 1){	//Existe um mÃ¡ximo.
			if(maximos[0] == 1){	//O maior Ã© o da esquerda.
				//Dirijo-me ao quadrado da direita.
				preencheFactores(xOvelha + 1, yOvelha);
				contaPassos(xOvelha + 1, yOvelha, false);
				aproximaOvelha();
				while(orientacao != Quadrado.ESQUERDA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avnacar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else if(maximos[1] == 1){	//O maior Ã© o da direita.
				//Dirijo-me ao quadrado da esquerda.
				
				preencheFactores(xOvelha - 1, yOvelha);
				if(yOvelha > 0 && contaPassos(xOvelha - 1, yOvelha, false) < 100)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
	
					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha, yOvelha - 1);
					if(yOvelha > 0 && contaPassos(xOvelha, yOvelha - 1, false) < 100)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.CIMA)
						{
							roda();
						}
		
						//Age sobre ovelha
						getMotorsStop();
						if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
						{
							grita();
							atualizaPosicaoOvelha(numOvelha, 1);
						}
						else
						{
							toca();
							atualizaPosicaoOvelha(numOvelha, 2);
						}
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();//Fica a esperada jogada da ovelha
					}
					else
					{
						preencheFactores(xOvelha + 1, yOvelha);
						if(xOvelha < 5 && contaPassos(xOvelha + 1, yOvelha, false) < 100)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.ESQUERDA)
							{
								roda();
							}
			
							//Age sobre ovelha
							getMotorsStop();
							if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
							{
								grita();
								atualizaPosicaoOvelha(numOvelha, 1);
							}
							else
							{
								toca();
								atualizaPosicaoOvelha(numOvelha, 2);
							}
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();//Fica a esperada jogada da ovelha
						}
						else
						{
							preencheFactores(xOvelha, yOvelha + 1);
							if(yOvelha < 5 && contaPassos(xOvelha, yOvelha + 1, false) < 100)
							{
								aproximaOvelha();
								while(orientacao != Quadrado.BAIXO)
								{
									roda();
								}
				
								//Age sobre ovelha
								getMotorsStop();
								if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
								{
									grita();
									atualizaPosicaoOvelha(numOvelha, 1);
								}
								else
								{
									toca();
									atualizaPosicaoOvelha(numOvelha, 2);
								}
								System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
								Button.waitForAnyPress();//Fica a esperada jogada da ovelha
							}
						}
					}
				}
				
				
			}
			else if(maximos[2] == 1){	//O maior Ã© o de cima.
				//Dirijo-me ao quadrado de baixo.
				preencheFactores(xOvelha, yOvelha - 1);
				if(yOvelha > 0 && contaPassos(xOvelha, yOvelha - 1, false) <= 2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}
					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha - 1, yOvelha);
					if(xOvelha < 5 && contaPassos(xOvelha - 1, yOvelha, false) <= 2)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.DIREITA)
						{
							roda();
						}
						//Age sobre ovelha
						getMotorsStop();
						if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
						{
							grita();
							atualizaPosicaoOvelha(numOvelha, 1);
						}
						else
						{
							toca();
							atualizaPosicaoOvelha(numOvelha, 2);
						}
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();
						//Fica a esperada jogada da ovelha
					}
					else
					{
						preencheFactores(xOvelha + 1, yOvelha);
						if(xOvelha < 5 && contaPassos(xOvelha + 1, yOvelha, false) <= 2)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.ESQUERDA)
							{
								roda();
							}
							//Age sobre ovelha
							getMotorsStop();
							if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
							{
								grita();
								atualizaPosicaoOvelha(numOvelha, 1);
							}
							else
							{
								toca();
								atualizaPosicaoOvelha(numOvelha, 2);
							}
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();
							//Fica a esperada jogada da ovelha
						}
					}
				}
			}
			else if(maximos[3] == 1){	//O maior Ã© o de baixo.
				//Dirijo-me ao quadrado de cima.
				preencheFactores(xOvelha, yOvelha + 1);
				contaPassos(xOvelha, yOvelha + 1, false);
				aproximaOvelha();
				while(orientacao != Quadrado.BAIXO)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
		}
		else if(maximos[4] == 2 || maximos[4] == 3 || maximos[4] == 4){	//Existem 2 mÃ¡ximos, cima = direita.
			preencheFactores(xOvelha - 1, yOvelha);
			if(posicaoRobotX == 0 && posicaoRobotY == 0)
			{
				preencheFactores(xOvelha - 1, yOvelha);
				if(xOvelha > 0 && contaPassos(xOvelha - 1, yOvelha, false) < 100)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.DIREITA)
					{
						roda();
					}
	
					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha, yOvelha - 1);
					if(yOvelha > 0 && contaPassos(xOvelha, yOvelha - 1, false) < 100)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.CIMA)
						{
							roda();
						}
		
						//Age sobre ovelha
						getMotorsStop();
						if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
						{
							grita();
							atualizaPosicaoOvelha(numOvelha, 1);
						}
						else
						{
							toca();
							atualizaPosicaoOvelha(numOvelha, 2);
						}
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();//Fica a esperada jogada da ovelha
					}
					else
					{
						preencheFactores(xOvelha + 1, yOvelha);
						if(xOvelha < 5 && contaPassos(xOvelha + 1, yOvelha, false) < 100)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.ESQUERDA)
							{
								roda();
							}
			
							//Age sobre ovelha
							getMotorsStop();
							if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
							{
								grita();
								atualizaPosicaoOvelha(numOvelha, 1);
							}
							else
							{
								toca();
								atualizaPosicaoOvelha(numOvelha, 2);
							}
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();//Fica a esperada jogada da ovelha
						}
						else
						{
							preencheFactores(xOvelha, yOvelha + 1);
							if(yOvelha < 5 && contaPassos(xOvelha, yOvelha + 1, false) < 100)
							{
								aproximaOvelha();
								while(orientacao != Quadrado.BAIXO)
								{
									roda();
								}
				
								//Age sobre ovelha
								getMotorsStop();
								if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
								{
									grita();
									atualizaPosicaoOvelha(numOvelha, 1);
								}
								else
								{
									toca();
									atualizaPosicaoOvelha(numOvelha, 2);
								}
								System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
								Button.waitForAnyPress();//Fica a esperada jogada da ovelha
							}
						}
					}
				}
			}
			else if(contaPassos(xOvelha - 1, yOvelha, true) <= 2)
			{
				aproximaOvelha();
				while(orientacao != Quadrado.DIREITA)
				{
					roda();
				}

				//Age sobre ovelha
				getMotorsStop();
				if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
				{
					grita();
					atualizaPosicaoOvelha(numOvelha, 1);
				}
				else
				{
					toca();
					atualizaPosicaoOvelha(numOvelha, 2);
				}
				System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
				Button.waitForAnyPress();
				//Fica a esperada jogada da ovelha
			}
			else
			{
				preencheFactores(xOvelha , yOvelha - 1);
				if(yOvelha > 0 && contaPassos(xOvelha, yOvelha - 1, true) <= 2)
				{
					aproximaOvelha();
					while(orientacao != Quadrado.CIMA)
					{
						roda();
					}

					//Age sobre ovelha
					getMotorsStop();
					if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
					{
						grita();
						atualizaPosicaoOvelha(numOvelha, 1);
					}
					else
					{
						toca();
						atualizaPosicaoOvelha(numOvelha, 2);
					}
					System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
					Button.waitForAnyPress();
					//Fica a esperada jogada da ovelha
				}
				else
				{
					preencheFactores(xOvelha + 1, yOvelha);
					if(xOvelha < 5 && contaPassos(xOvelha + 1, yOvelha, true) <= 2)
					{
						aproximaOvelha();
						while(orientacao != Quadrado.ESQUERDA)
						{
							roda();
						}

						//Age sobre ovelha
						getMotorsStop();
						if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
						{
							grita();
							atualizaPosicaoOvelha(numOvelha, 1);
						}
						else
						{
							toca();
							atualizaPosicaoOvelha(numOvelha, 2);
						}
						System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
						Button.waitForAnyPress();
						//Fica a esperada jogada da ovelha
					}
					else
					{
						preencheFactores(xOvelha, yOvelha + 1);
						if(yOvelha < 5 && contaPassos(xOvelha, yOvelha + 1, true) <= 2)
						{
							aproximaOvelha();
							while(orientacao != Quadrado.BAIXO)
							{
								roda();
							}

							//Age sobre ovelha
							getMotorsStop();
							if(contaOvelhasVolta(posicaoRobotX, posicaoRobotY) < 2)
							{
								grita();
								atualizaPosicaoOvelha(numOvelha, 1);
							}
							else
							{
								toca();
								atualizaPosicaoOvelha(numOvelha, 2);
							}
							System.out.println("A espera da jogada da Ovelha. Prima em qualquer botao quando eu puder avancar.");
							Button.waitForAnyPress();
							//Fica a esperada jogada da ovelha
						}
						else
						{
							System.out.println("Fiquei bloqueado.");
						}
					}
				}
			}
		}


		return;
	}
	

	/**
	 * Retorna um array com 5 posiÃ§Ãµes {esq, dir, cima, baixo, contaMaximos}.
	 * Os valores a 1 sÃ£o os mÃ¡ximos.
	 * O contaMaximos Ã© um inteiro que conta quantos mÃ¡ximos existe no array.
	 */
	private int[] checkMax(int esq, int dir, int cima, int baixo){
		int[] nums = new int [5];

		int max = Math.max(Math.max(Math.max(esq, dir), cima), baixo);
		int contaMax = 0;

		if(esq == max)
		{
			nums[0]=0;
		}
		else
		{
			nums[0] = 0;
		}
		if(dir == max)
		{
			nums[1] = 1;
		}
		else
		{
			nums[1] = 0;
		}
		if(cima == max)
		{
			nums[2] = 1;
		}
		else
		{
			nums[2] = 0;
		}
		if(baixo == max)
		{
			nums[3] = 1;
		}
		else
		{
			nums[3] = 0;
		}

		for(Integer i : nums){
			if(i == 1) contaMax++;
		}

		nums[4] = contaMax;
		return nums;
	}
	
	/**
	 * Atualiza a posicao da ovelhao ao fim de cada jogada
	 * Recebe um bollean q deve ser true qd o robot tocou e um inteiro que indica se se trata da ovelha 1 ou 2
	 * @param tocou
	 * @param numOvelha
	 */
	private int[] atualizaPosicaoOvelha(int numOvelha, int numPassos)
	{
		int xOvelha;
		int yOvelha;
		boolean mesmoQuadrado = false;
		
		int [] novasPosicoes = new int [2];
		int [] guardaPosOvelha = new int [2];
		
		if(posicaoOvelha1[0] == posicaoOvelha2[0] && posicaoOvelha1[1] == posicaoOvelha2[1])
		{
			mesmoQuadrado = true;
		}
				
		if(numOvelha == 1)
		{
			guardaPosOvelha[0] = posicaoOvelha1[0];
			guardaPosOvelha[1] = posicaoOvelha1[1];
		
			xOvelha = posicaoOvelha1[0];
			yOvelha = posicaoOvelha1[1];
		}
		else
		{
			guardaPosOvelha[0] = posicaoOvelha2[0];
			guardaPosOvelha[1] = posicaoOvelha2[1];
			
			xOvelha = posicaoOvelha2[0];
			yOvelha = posicaoOvelha2[1];
		}
		
		tb.getTabuleiro()[xOvelha][yOvelha].setOvelha(false);
		
		if(posicaoRobotX < xOvelha)
		{
			novasPosicoes = rob_esq_ove( numPassosOvelha,  xOvelha, yOvelha,  guardaPosOvelha, numPassos);
		}
		else if(posicaoRobotY < yOvelha)
		{
			novasPosicoes = rob_bai_ove( numPassosOvelha,  xOvelha, yOvelha,  guardaPosOvelha, numPassos);
		}
		else if(posicaoRobotX > xOvelha)
		{
			novasPosicoes =rob_dir_ove( numPassosOvelha,  xOvelha, yOvelha,  guardaPosOvelha, numPassos);
		}
		else if(posicaoRobotY > yOvelha)
		{
			novasPosicoes = rob_cim_ove( numPassosOvelha,  xOvelha, yOvelha,  guardaPosOvelha, numPassos);
		}
		
		if(numOvelha == 1)
		{
			posicaoOvelha1[0] = novasPosicoes[0];
			posicaoOvelha1[1] = novasPosicoes[1];
			tb.getTabuleiro()[posicaoOvelha1[0]][posicaoOvelha1[1]].setOvelha(true);}
		else
		{
			posicaoOvelha2[0] = novasPosicoes[0];
			posicaoOvelha2[1] = novasPosicoes[1];
			tb.getTabuleiro()[posicaoOvelha2[0]][posicaoOvelha2[1]].setOvelha(true);
		}
		if(mesmoQuadrado && numOvelha == 1)
		{
			posicaoOvelha2[0] = posicaoOvelha1[0];
			posicaoOvelha2[1] = posicaoOvelha1[1];
		}
		else if(mesmoQuadrado && numOvelha == 2)
		{
			posicaoOvelha1[0] = posicaoOvelha2[0];
			posicaoOvelha1[1] = posicaoOvelha2[1];
		}
		numPassosOvelha = 0;
		System.out.println("X1 " + posicaoOvelha1[0] + " Y1 " + posicaoOvelha1[1]);
		System.out.println("X2 " + posicaoOvelha2[0] + " Y " + posicaoOvelha2[1]);
		
		return novasPosicoes;
	}
	
	/**
	 * Verifica se o robot está a baixo da ovelha e actualiza a posição da ovelha tendo em conta o toque ou o grita e a posicao onde  voelha se encontra
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] rob_bai_ove(int numPassosOvelha, int xOvelha, int yOvelha, int[] guardaPosOvelha, int numPassos)
	{
		int [] pos = new int [2];
		if(numPassosOvelha != numPassos)
		{
			//guardaPosOvelha[0] = xOvelha;
			//guardaPosOvelha[1] = yOvelha;
			pos = barr_acima_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			pos[0] = xOvelha;
			pos[1] = yOvelha;
		}
		return pos;
	}
	
	/**
	 * Verifica se o robot está acima da ovelha e actualiza a posição da ovelha tendo em conta o toque ou o grita e a posicao onde  voelha se encontra
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] rob_cim_ove(int numPassosOvelha, int xOvelha, int yOvelha, int[] guardaPosOvelha, int numPassos)
	{
		int [] pos = new int [2];
		if(numPassosOvelha != numPassos)
		{
			pos = barr_bai_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			pos[0] = xOvelha;
			pos[1] = yOvelha;
		}
		return pos;
	}
	
	/**
	 * Verifica se o robot está a esquerda da ovelha e actualiza a posição da ovelha tendo em conta o toque ou o grita e a posicao onde  voelha se encontra
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] rob_esq_ove(int numPassosOvelha, int xOvelha, int yOvelha, int[] guardaPosOvelha, int numPassos)
	{
		int [] pos = new int [2];
		if(numPassosOvelha != numPassos)
		{
			pos = barr_dir_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			pos[0] = xOvelha;
			pos[1] = yOvelha;
		}
		return pos;
	}
	/**
	 * Verifica se o robot está a direita da ovelha e actualiza a posição da ovelha tendo em conta o toque ou o grita e a posicao onde  voelha se encontra
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] rob_dir_ove(int numPassosOvelha, int xOvelha, int yOvelha, int[] guardaPosOvelha, int numPassos)
	{
		int [] pos = new int [2];
		if(numPassosOvelha != numPassos)
		{
			pos = barr_esq_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			pos[0] = xOvelha;
			pos[1] = yOvelha;
		}
		return pos;
	}
	
	/**
	 * Metodo auxiliar que verifica se existe barreira a cima da ovelha.
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] barr_acima_ove(int numPassosOvelha, int xOvelha, int yOvelha, int []guardaPosOvelha, int numPassos)
	{
		int [] novasPosicoes = new int[2];
		if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.CIMA])
		{
			novasPosicoes = barr_dir_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			if((posicaoRobotX == xOvelha && posicaoRobotY == yOvelha + 1 ) || (xOvelha == guardaPosOvelha[0]  && yOvelha + 1== guardaPosOvelha[1] && numPassosOvelha == 1))
			{
				novasPosicoes = barr_dir_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
			else
			{
				guardaPosOvelha[1] = yOvelha;
				yOvelha ++;
				numPassosOvelha++;
				novasPosicoes = rob_bai_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
		}
		return novasPosicoes;
	}
	
	/**
	 * Metodo auxiliar que verifica se existe barreira em baixo da ovelha.
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] barr_bai_ove(int numPassosOvelha, int xOvelha, int yOvelha, int []guardaPosOvelha, int numPassos)
	{
		int [] novasPosicoes = new int[2];
		if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.BAIXO])
		{
			novasPosicoes = barr_esq_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			if((posicaoRobotX == xOvelha && posicaoRobotY == yOvelha - 1 ) || (xOvelha == guardaPosOvelha[0]  && yOvelha - 1 == guardaPosOvelha[1] && numPassosOvelha == 1))
			{
				novasPosicoes = barr_esq_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
			else
			{
				guardaPosOvelha[1] = yOvelha;
				yOvelha --;
				numPassosOvelha++;
				novasPosicoes = rob_cim_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
		}
		return novasPosicoes;
	}
	
	/**
	 * Metodo auxiliar que verifica se existe barreira a esquerda da ovelha.
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] barr_esq_ove(int numPassosOvelha, int xOvelha, int yOvelha, int []guardaPosOvelha, int numPassos)
	{
		int [] novasPosicoes = new int[2];
		if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.ESQUERDA])
		{
			novasPosicoes = barr_acima_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			if((posicaoRobotX == xOvelha - 1 && posicaoRobotY == yOvelha ) || (xOvelha - 1 == guardaPosOvelha[0]  && yOvelha == guardaPosOvelha[1] && numPassosOvelha == 1))
			{
				novasPosicoes = barr_acima_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
			else
			{
				guardaPosOvelha[0] = xOvelha;
				xOvelha --;
				numPassosOvelha++;
				novasPosicoes = rob_dir_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
		}
		return novasPosicoes;
	}
	/**
	 * Metodo auxiliar que verifica se existe barreira a direita da ovelha.
	 * @param numPassosOvelha
	 * @param xOvelha
	 * @param yOvelha
	 * @param guardaPosOvelha
	 * @param numPassos
	 * @return
	 */
	private int[] barr_dir_ove(int numPassosOvelha, int xOvelha, int yOvelha, int []guardaPosOvelha, int numPassos)
	{
		int[] novasPosicoes = new int[2];
		if(tb.getTabuleiro()[xOvelha][yOvelha].getBarreiraL()[Quadrado.DIREITA])
		{
			novasPosicoes = barr_bai_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
		}
		else
		{
			if((posicaoRobotX == xOvelha + 1 && posicaoRobotY == yOvelha ) || (xOvelha + 1 == guardaPosOvelha[0]  && yOvelha == guardaPosOvelha[1] && numPassosOvelha == 1))
			{
				novasPosicoes = barr_bai_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
			else
			{
				guardaPosOvelha[0] = xOvelha;
				xOvelha ++;
				numPassosOvelha++;
				novasPosicoes = rob_esq_ove(numPassosOvelha,xOvelha,yOvelha,guardaPosOvelha, numPassos);
			}
		}
		return novasPosicoes;
	}
}

