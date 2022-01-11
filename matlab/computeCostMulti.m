function J = computeCostMulti(X, y, theta, lambda)

m = length(y);

J = 0;

e = (X * theta - y);

theta_one = theta;
theta_one(1) = 0;

% J = (e' * e) / (2 * m);
J = (e' * e) / (2 * m) + lambda .* (theta_one' * theta_one);


end